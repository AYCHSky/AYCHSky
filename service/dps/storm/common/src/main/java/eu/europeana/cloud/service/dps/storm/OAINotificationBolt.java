package eu.europeana.cloud.service.dps.storm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is just temporary class that has exactly same behaviour like @{@link NotificationBolt}.
 * Only one difference is that this @{@link OAINotificationBolt} will use another table (processed_records)
 * for storing records statuses.
 *
 * In the future all other topologies will use this table (processed_records). So there two implementations of
 * Notification bolt will be unified to one solution.
 *
 */
public class OAINotificationBolt extends NotificationBolt {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAINotificationBolt.class);

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
    public OAINotificationBolt(String hosts, int port, String keyspaceName, String userName, String password) {
        super(hosts, port, keyspaceName, userName, password);
    }


    @Override
    protected void insertRecordDetailedInformation(int resourceNum, long taskId, String resource, String state, String infoText, String additionalInfo, String resultResource) {
        int retries = DEFAULT_RETRIES;

        while (true) {
            try {
                processedRecordsDAO.insert(taskId, resource, resultResource, topologyName, state, infoText, additionalInfo);
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

}
