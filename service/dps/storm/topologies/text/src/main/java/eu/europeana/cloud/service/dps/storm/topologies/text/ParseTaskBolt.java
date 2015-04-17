package eu.europeana.cloud.service.dps.storm.topologies.text;

import java.io.IOException;
import java.util.HashMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.dps.storm.StormTupleKeys;

public class ParseTaskBolt extends BaseBasicBolt 
{
    public static final Logger LOGGER = LoggerFactory.getLogger(ParseTaskBolt.class);

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) 
    {
        declarer.declare(new Fields(
                StormTupleKeys.TASK_ID_TUPLE_KEY,
                StormTupleKeys.TASK_NAME_TUPLE_KEY,
                StormTupleKeys.INPUT_FILES_TUPLE_KEY,
                StormTupleKeys.FILE_CONTENT_TUPLE_KEY,
                StormTupleKeys.PARAMETERS_TUPLE_KEY));
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) 
    {
        ObjectMapper mapper = new ObjectMapper();
        DpsTask task = null;
        try 
        {
            task = mapper.readValue(tuple.getString(0), DpsTask.class);
        } 
        catch (IOException e) 
        {
            LOGGER.error("Message '{}' rejected because: {}", tuple.getString(0), e.getMessage());
            return;
        }

        HashMap<String, String> taskParameters = task.getParameters();
        //HashMap<String, List<String>> inputData = task.getInputData();

        StormTaskTuple stormTaskTuple = new StormTaskTuple(
                task.getTaskId(), 
                task.getTaskName(), 
                null, null, taskParameters);
        
        if(taskParameters != null)
        {
            LOGGER.info("taskParameters size=" + taskParameters.size());
        }
        
        collector.emit(stormTaskTuple.toStormTuple());
    }
}
