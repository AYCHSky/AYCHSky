package eu.europeana.cloud.service.dps.storm;


import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import eu.europeana.cloud.cassandra.CassandraConnectionProvider;
import eu.europeana.cloud.cassandra.CassandraConnectionProviderSingleton;
import eu.europeana.cloud.common.model.dps.RecordState;
import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.exception.DatabaseConnectionException;
import eu.europeana.cloud.service.dps.exception.TaskInfoDoesNotExistException;
import eu.europeana.cloud.service.dps.storm.utils.CassandraSubTaskInfoDAO;
import eu.europeana.cloud.service.dps.storm.utils.CassandraTaskErrorsDAO;
import eu.europeana.cloud.service.dps.storm.utils.CassandraTaskInfoDAO;
import eu.europeana.cloud.service.dps.storm.utils.ProcessedRecordsDAO;
import eu.europeana.cloud.service.dps.util.LRUCache;
import org.apache.commons.lang3.Validate;
import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This bolt is responsible for store notifications to Cassandra.
 *
 * @author Pavel Kefurt <Pavel.Kefurt@gmail.com>
 */
public class NotificationBolt extends BaseRichBolt {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationBolt.class);

    protected Map stormConfig;
    protected TopologyContext topologyContext;
    protected OutputCollector outputCollector;

    private final String hosts;
    private final int port;
    private final String keyspaceName;
    private final String userName;
    private final String password;
    protected static LRUCache<Long, NotificationCache> cache = new LRUCache<Long, NotificationCache>(
            50);

    protected String topologyName;
    private static CassandraConnectionProvider cassandraConnectionProvider;
    protected static CassandraTaskInfoDAO taskInfoDAO;
    private static CassandraSubTaskInfoDAO subTaskInfoDAO;
    protected ProcessedRecordsDAO processedRecordsDAO;
    private static CassandraTaskErrorsDAO taskErrorDAO;
    private static final int COUNTER_UPDATE_INTERVAL_IN_MS = 5 * 1000;

    protected static final int DEFAULT_RETRIES = 3;

    protected static final int SLEEP_TIME = 5000;

    /**
     * Constructor of notification bolt.
     *
     * @param hosts        Cassandra hosts separated by comma (e.g.
     *                     localhost,192.168.47.129)
     * @param port         Cassandra port
     * @param keyspaceName Cassandra keyspace name
     * @param userName     Cassandra username
     * @param password     Cassandra password
     */
    public NotificationBolt(String hosts, int port, String keyspaceName,
                            String userName, String password) {
        this.hosts = hosts;
        this.port = port;
        this.keyspaceName = keyspaceName;
        this.userName = userName;
        this.password = password;

    }

    @Override
    public void execute(Tuple tuple) {
        try {
            NotificationTuple notificationTuple = NotificationTuple
                    .fromStormTuple(tuple);
            NotificationCache nCache = cache.get(notificationTuple.getTaskId());
            if (nCache == null) {
                nCache = new NotificationCache();
                cache.put(notificationTuple.getTaskId(), nCache);
            }
            storeTaskDetails(notificationTuple, nCache);

        } catch (NoHostAvailableException | QueryExecutionException ex) {
            LOGGER.error("Cannot store notification to Cassandra because: {}",
                    ex.getMessage());
            return;
        } catch (Exception ex) {
            LOGGER.error("Problem with store notification because: {}",
                    ex.getMessage());
            return;
        }
    }

    private void storeTaskDetails(NotificationTuple notificationTuple, NotificationCache nCache) throws TaskInfoDoesNotExistException, DatabaseConnectionException {
        long taskId = notificationTuple.getTaskId();
        switch (notificationTuple.getInformationType()) {
            case UPDATE_TASK:
                updateTask(taskId,
                        notificationTuple.getParameters());
                break;
            case NOTIFICATION:
                notifyTask(notificationTuple, nCache, taskId);
                storeFinishState(notificationTuple);
                break;
        }
    }

    private void notifyTask(NotificationTuple notificationTuple, NotificationCache nCache, long taskId) throws DatabaseConnectionException, TaskInfoDoesNotExistException {
        boolean error = isError(notificationTuple, nCache);

        int processesFilesCount = nCache.getProcessed();
        int errors = nCache.getErrors();

        //notification table (for single record)
        storeNotification(processesFilesCount, taskId,
                notificationTuple.getParameters());

        if (error) {
            storeNotificationError(taskId, nCache, notificationTuple);
        }
        if(isCounterUpdateRequired(nCache)){
            LOGGER.info("Updating task counter for task_id = {} and counter value: {}", taskId, processesFilesCount);
            taskInfoDAO.setUpdateProcessedFiles(taskId, processesFilesCount, errors);
            nCache.setLastCounterUpdate(new Date());
        }
    }

    private boolean isCounterUpdateRequired(NotificationCache nCache) {
        return new Date().getTime() - nCache.getLastCounterUpdate().getTime() > COUNTER_UPDATE_INTERVAL_IN_MS;
    }

    private void storeNotificationError(long taskId, NotificationCache nCache, NotificationTuple notificationTuple) {
        Map<String, Object> parameters = notificationTuple.getParameters();
        Validate.notNull(parameters);
        String errorMessage = String.valueOf(parameters.get(NotificationParameterKeys.INFO_TEXT));
        String additionalInformation = String.valueOf(parameters.get(NotificationParameterKeys.ADDITIONAL_INFORMATIONS));
        if (!String.valueOf(notificationTuple.getParameters().get(NotificationParameterKeys.STATE)).equalsIgnoreCase(RecordState.ERROR.toString()) && parameters.get(PluginParameterKeys.UNIFIED_ERROR_MESSAGE) != null) {
            errorMessage = String.valueOf(parameters.get(NotificationParameterKeys.UNIFIED_ERROR_MESSAGE));
            additionalInformation = String.valueOf(parameters.get(NotificationParameterKeys.EXCEPTION_ERROR_MESSAGE));
        }
        String errorType = nCache.getErrorType(errorMessage);
        String resource = String.valueOf(parameters.get(NotificationParameterKeys.RESOURCE));
        updateErrorCounter(taskId, errorType);
        insertError(taskId, errorMessage, additionalInformation, errorType, resource);
    }

    private void updateErrorCounter(long taskId, String errorType) {
        int retries = DEFAULT_RETRIES;
        while (true) {
            try {
                taskErrorDAO.updateErrorCounter(taskId, errorType);
                break;
            } catch (Exception e) {
                if (retries-- > 0) {
                    LOGGER.warn("Error while updating Error counter. Retries left: {}", retries);
                    waitForSpecificTime();
                } else {
                    LOGGER.error("Error while updating Error counter.");
                    throw e;
                }
            }
        }
    }

    private void insertError(long taskId, String errorMessage, String additionalInformation, String errorType, String resource) {
        int retries = DEFAULT_RETRIES;

        while (true) {
            try {
                taskErrorDAO.insertError(taskId, errorType, errorMessage, resource, additionalInformation);
                break;
            } catch (Exception e) {
                if (retries-- > 0) {
                    LOGGER.warn("Error while inserting Error to cassandra. Retries left: {}", retries);
                    waitForSpecificTime();
                } else {
                    LOGGER.error("Error while inserting Error to cassandra.");
                    throw e;
                }
            }
        }
    }

    protected void waitForSpecificTime() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            LOGGER.error(e1.getMessage());
        }
    }


    private boolean isError(NotificationTuple notificationTuple, NotificationCache nCache) {
        if (String.valueOf(notificationTuple.getParameters().get(NotificationParameterKeys.STATE)).equalsIgnoreCase(RecordState.ERROR.toString())) {
            nCache.inc(true);
            return true;
        } else if (notificationTuple.getParameter(PluginParameterKeys.UNIFIED_ERROR_MESSAGE) != null) {
            nCache.inc(false);
            return true;
        }
        nCache.inc(false);
        return false;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext tc, OutputCollector oc) {
        cassandraConnectionProvider = CassandraConnectionProviderSingleton.getCassandraConnectionProvider(hosts, port, keyspaceName,
                userName, password);
        taskInfoDAO = CassandraTaskInfoDAO.getInstance(cassandraConnectionProvider);
        subTaskInfoDAO = CassandraSubTaskInfoDAO.getInstance(cassandraConnectionProvider);
        processedRecordsDAO = ProcessedRecordsDAO.getInstance(cassandraConnectionProvider);
        taskErrorDAO = CassandraTaskErrorsDAO.getInstance(cassandraConnectionProvider);
        topologyName = (String) stormConf.get(Config.TOPOLOGY_NAME);
        this.stormConfig = stormConf;
        this.topologyContext = tc;
        this.outputCollector = oc;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer ofd) {

    }

    private void updateTask(long taskId, Map<String, Object> parameters) throws DatabaseConnectionException {
        Validate.notNull(parameters);
        String state = String.valueOf(parameters.get(NotificationParameterKeys.TASK_STATE));
        String info = String.valueOf(parameters.get(NotificationParameterKeys.INFO));
        Date startDate = prepareDate(parameters.get(NotificationParameterKeys.START_TIME));
        updateTask(taskId, state, info, startDate);
    }

    private void updateTask(long taskId, String state, String info, Date startDate) {
        int retries = DEFAULT_RETRIES;

        while (true) {
            try {
                taskInfoDAO.updateTask(taskId, info, state, startDate);
                break;
            } catch (Exception e) {
                if (retries-- > 0) {
                    LOGGER.warn("Error while Updating the task info. Retries left: {}", retries);
                    waitForSpecificTime();
                } else {
                    LOGGER.error("Error while Updating the task info.");
                    throw e;
                }
            }
        }
    }

    private static Date prepareDate(Object dateObject) {
        Date date = null;
        if (dateObject instanceof Date)
            return (Date) dateObject;
        return date;
    }

    private void storeFinishState(NotificationTuple notificationTuple) throws TaskInfoDoesNotExistException, DatabaseConnectionException {
        long taskId = notificationTuple.getTaskId();
        TaskInfo task = taskInfoDAO.searchById(taskId);
        if (task != null) {
            NotificationCache nCache = cache.get(taskId);
            int count = nCache.getProcessed();
            int expectedSize = task.getExpectedSize();
            if (count == expectedSize) {
                endTask(notificationTuple, nCache.getErrors(), count);
            }
        }
    }

    protected void endTask(NotificationTuple notificationTuple, int errors, int count) {
        taskInfoDAO.endTask(notificationTuple.getTaskId(), count, errors, "Completely processed", String.valueOf(TaskState.PROCESSED), new Date());
    }

    private void storeNotification(int resourceNum, long taskId, Map<String, Object> parameters) throws DatabaseConnectionException {
        Validate.notNull(parameters);
        String resource = String.valueOf(parameters.get(NotificationParameterKeys.RESOURCE));
        String state = String.valueOf(parameters.get(NotificationParameterKeys.STATE));
        String infoText = String.valueOf(parameters.get(NotificationParameterKeys.INFO_TEXT));
        String additionalInfo = String.valueOf(parameters.get(NotificationParameterKeys.ADDITIONAL_INFORMATIONS));
        String resultResource = String.valueOf(parameters.get(NotificationParameterKeys.RESULT_RESOURCE));
        insertRecordDetailedInformation(resourceNum, taskId, resource, state, infoText, additionalInfo, resultResource);
    }

    protected void insertRecordDetailedInformation(int resourceNum, long taskId, String resource, String state, String infoText, String additionalInfo, String resultResource) {
        int retries = DEFAULT_RETRIES;

        while (true) {
            try {
                subTaskInfoDAO.insert(resourceNum, taskId, topologyName, resource, state, infoText, additionalInfo, resultResource);
                break;
            } catch (Exception e) {
                if (retries-- > 0) {
                    LOGGER.warn("Error while inserting detailed record information to cassandra. Retries left: {}", retries);
                    waitForSpecificTime();
                } else {
                    LOGGER.error("Error while inserting detailed record information to cassandra.");
                    throw e;
                }
            }
        }
    }

    public static void clearCache() {
        cache.clear();
    }

    protected static class NotificationCache {

        int processed = 0;
        int errors = 0;
        private Date lastCounterUpdate = new Date();

        Map<String, String> errorTypes = new HashMap<>();

        NotificationCache() {
        }

        public void inc(boolean error) {
            processed++;
            if (error) {
                errors++;
            }
        }

        public int getProcessed() {
            return processed;
        }

        public int getErrors() {
            return errors;
        }

        public String getErrorType(String infoText) {
            String errorType = errorTypes.get(infoText);
            if (errorType == null) {
                errorType = new com.eaio.uuid.UUID().toString();
                errorTypes.put(infoText, errorType);
            }
            return errorType;
        }

        Date getLastCounterUpdate(){
            return lastCounterUpdate;
        }

        void setLastCounterUpdate(Date lastCounterUpdate) {
            this.lastCounterUpdate = lastCounterUpdate;
        }
    }


}


