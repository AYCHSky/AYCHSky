package eu.europeana.cloud.service.dps.storm;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Tuple;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * This bolt is responsible for convert {@link DpsTask} to {@link StormTaskTuple} and it emits result to specific storm stream.
 * @author Pavel Kefurt <Pavel.Kefurt@gmail.com>
 */
public class ParseTaskBolt extends BaseBasicBolt 
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseTaskBolt.class);
    
    public static final String NOTIFICATION_STREAM_NAME = AbstractDpsBolt.NOTIFICATION_STREAM_NAME;
    
    public final Map<String, String> routingRules;
    private final Map<String, String> prerequisites;
    
    /**
     * Constructor for ParseTaskBolt without routing and conditions.
     */
    public ParseTaskBolt() 
    {
        this(null, null);
    }
  
    /**
     * Constructor for ParseTaskBolt with routing.
     * Task is dropped if TaskName is not in routingRules.
     * @param routingRules routing table in the form ("TaskName": "StreamName")
     * @param prerequisites Necessary parameters in DpsTask for continue. ("ParameterName": "CaseInsensitiveValue" or null if value is not important)
     *              If parameter name is set in this structure and is not set in DpsTask or has a different value, than DpsTask will be dropped.
     */
    public ParseTaskBolt(Map<String, String> routingRules, Map<String, String> prerequisites) 
    {
        this.routingRules = routingRules;
        this.prerequisites = prerequisites;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) 
    {
        if(routingRules != null)
        {
            for(Map.Entry<String, String> rule : routingRules.entrySet())
            {
                declarer.declareStream(rule.getValue(), StormTaskTuple.getFields());
            }
        }
        else
        {
            declarer.declare(StormTaskTuple.getFields());
        }
        
        declarer.declareStream(NOTIFICATION_STREAM_NAME, NotificationTuple.getFields());
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) 
    {
        ObjectMapper mapper = new ObjectMapper();
        DpsTask task;
        try 
        {
            task = mapper.readValue(tuple.getString(0), DpsTask.class);
        } 
        catch (IOException e) 
        {
            LOGGER.error("Message '{}' rejected because: {}", tuple.getString(0), e.getMessage());
            return;
        }

        Map<String, String> taskParameters = task.getParameters();
        
        //chceck necessary parameters for current topology
        if(prerequisites != null && taskParameters != null)
        {
            for(Map.Entry<String, String> importantParameter : prerequisites.entrySet())
            {
                String p = taskParameters.get(importantParameter.getKey());
                if(p != null)
                {
                    String val = importantParameter.getValue();
                    if(val != null && !val.toLowerCase().equals(p.toLowerCase()))
                    {
                        //values not equal => drop this task
                        LOGGER.info("DpsTask with id {} is dropped because parameter {} does not have a required value '{}'.", 
                            task.getTaskId(), importantParameter.getKey(), val);
                        
                        String message = String.format("Dropped because parameter %s does not have a required value '%s'.",
                                importantParameter.getKey(), val);
                        emitDropNotification(collector, task.getTaskId(), "", message, taskParameters.toString());
                        emitBasicInfo(collector, task.getTaskId(), 0);
                        return;
                    }
                }
                else    //parameter not exists => drop this task
                {
                    LOGGER.info("DpsTask with id {} is dropped because parameter {} is missing.", 
                            task.getTaskId(), importantParameter.getKey());
                    
                    String message = String.format("Dropped because parameter %s is missing.", importantParameter.getKey());
                    emitDropNotification(collector, task.getTaskId(), "", message, taskParameters.toString());
                    emitBasicInfo(collector, task.getTaskId(), 0);
                    return;
                }
            }
        }

        StormTaskTuple stormTaskTuple = new StormTaskTuple(
                task.getTaskId(), 
                task.getTaskName(), 
                null, null, taskParameters);
        
        if(taskParameters != null)
        {
            String fileUrl = taskParameters.get(PluginParameterKeys.FILE_URL);
            if(fileUrl != null && !fileUrl.isEmpty())
            {
                stormTaskTuple.setFileUrl(fileUrl);
            }
            
            String fileData = taskParameters.get(PluginParameterKeys.FILE_DATA);
            if(fileData != null && !fileData.isEmpty())
            {
                stormTaskTuple.setFileData(fileData);
            }
        }
        
        //add data from InputData as a parameter
        Map<String, List<String>> inputData = task.getInputData();
        if(inputData != null && !inputData.isEmpty())
        {
            Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
            stormTaskTuple.addParameter(PluginParameterKeys.DPS_TASK_INPUT_DATA, new Gson().toJson(inputData, type));
        }
        
        //use specific streams or default strem?
        if(routingRules != null)
        {
            String stream = routingRules.get(task.getTaskName());
            if(stream != null)
            {
                collector.emit(stream, stormTaskTuple.toStormTuple());
            }
            else
            {
                emitDropNotification(collector, task.getTaskId(), "", "Unknown task name.", 
                        taskParameters != null ? taskParameters.toString() : "");
                emitBasicInfo(collector, task.getTaskId(), 0);
            }
        }
        else
        {
            collector.emit(stormTaskTuple.toStormTuple());
        }
    }
    
    private void emitDropNotification(BasicOutputCollector collector, long taskId, 
            String resource, String message, String additionalInformations)
    {
        NotificationTuple nt = NotificationTuple.prepareNotification(taskId, 
                resource, NotificationTuple.States.DROPPED, message, additionalInformations);
        collector.emit(NOTIFICATION_STREAM_NAME, nt.toStormTuple());
    }
    
    protected void emitBasicInfo(BasicOutputCollector collector, long taskId, int expectedSize)
    {
        NotificationTuple nt = NotificationTuple.prepareBasicInfo(taskId, expectedSize);
        collector.emit(NOTIFICATION_STREAM_NAME, nt.toStormTuple());
    }
}
