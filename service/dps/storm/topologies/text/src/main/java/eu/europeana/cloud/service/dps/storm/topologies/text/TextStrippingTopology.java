package eu.europeana.cloud.service.dps.storm.topologies.text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.testing.FeederSpout;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.storm.ParseTaskBolt;
import eu.europeana.cloud.service.dps.storm.ProgressBolt;
import eu.europeana.cloud.service.dps.storm.io.ReadDatasetBolt;
import eu.europeana.cloud.service.dps.storm.io.ReadFileBolt;
import eu.europeana.cloud.service.dps.storm.io.StoreFileAsNewRepresentationBolt;
import eu.europeana.cloud.service.dps.storm.kafka.KafkaMetricsConsumer;
import eu.europeana.cloud.service.dps.storm.kafka.KafkaProducerBolt;

/**
 *
 * @author Pavel Kefurt <Pavel.Kefurt@gmail.com>
 */
public class TextStrippingTopology 
{
    public enum SpoutType
    {
        KAFKA,
        FEEDER
    }
    
    private final SpoutType spoutType;
    
    private final String datasetStream = "ReadDataset";
    private final String fileStream = "ReadFile";
    private final String storeStream = "StoreStream";
    private final String informStream = "InformStream";
    
    private final String zkProgressAddress = TextStrippingConstants.PROGRESS_ZOOKEEPER;
    private final String ecloudMcsAddress = TextStrippingConstants.MCS_URL;
    private final String username = TextStrippingConstants.USERNAME;
    private final String password = TextStrippingConstants.PASSWORD;
    
    /**
     * 
     * @param spoutType
     */
    public TextStrippingTopology(SpoutType spoutType) 
    {
        this.spoutType = spoutType;
    }
    
    protected StormTopology buildTopology() 
    {
        Map<String, String> routingRules = new HashMap<>();
        routingRules.put(PluginParameterKeys.NEW_DATASET_MESSAGE, datasetStream);
        routingRules.put(PluginParameterKeys.NEW_FILE_MESSAGE, fileStream);
        
        Map<String, String> prerequisites = new HashMap<>();
        prerequisites.put(PluginParameterKeys.EXTRACT_TEXT, "True");
        
        Map<String, String> outputParameters = new HashMap<>();
        outputParameters.put(PluginParameterKeys.INDEX_DATA, null);
        outputParameters.put(PluginParameterKeys.CLOUD_ID, null);
        outputParameters.put(PluginParameterKeys.REPRESENTATION_NAME, null);
        outputParameters.put(PluginParameterKeys.REPRESENTATION_VERSION, null);
        outputParameters.put(PluginParameterKeys.FILE_NAME, null);
        outputParameters.put(PluginParameterKeys.ORIGINAL_FILE_URL, null);
        outputParameters.put(PluginParameterKeys.FILE_METADATA, null);
        outputParameters.put(PluginParameterKeys.ELASTICSEARCH_INDEX, null);
        outputParameters.put(PluginParameterKeys.ELASTICSEARCH_TYPE, null);
        
        TopologyBuilder builder = new TopologyBuilder();
        
        builder.setSpout("KafkaSpout", getSpout(), TextStrippingConstants.KAFKA_SPOUT_PARALLEL);
        
        builder.setBolt("ParseDpsTask", new ParseTaskBolt(routingRules, prerequisites), TextStrippingConstants.PARSE_TASKS_BOLT_PARALLEL)
                .shuffleGrouping("KafkaSpout");
        
        builder.setBolt("RetrieveDataset", new ReadDatasetBolt(zkProgressAddress, ecloudMcsAddress, username, password), 
                            TextStrippingConstants.DATASET_BOLT_PARALLEL)
                .shuffleGrouping("ParseDpsTask", datasetStream);
        
        builder.setBolt("RetrieveFile", new ReadFileBolt(zkProgressAddress, ecloudMcsAddress, username, password), 
                            TextStrippingConstants.FILE_BOLT_PARALLEL)
                .shuffleGrouping("ParseDpsTask", fileStream);
        
        builder.setBolt("ExtractText", new ExtractTextBolt(storeStream, informStream), TextStrippingConstants.EXTRACT_BOLT_PARALLEL)
                .shuffleGrouping("RetrieveDataset")
                .shuffleGrouping("RetrieveFile");
        
        builder.setBolt("StoreNewRepresentation", new StoreFileAsNewRepresentationBolt(zkProgressAddress, ecloudMcsAddress, username, password), 
                            TextStrippingConstants.STORE_BOLT_PARALLEL)
                .shuffleGrouping("ExtractText", storeStream);
        
        builder.setBolt("InformBolt", new KafkaProducerBolt(TextStrippingConstants.KAFKA_OUTPUT_BROKER, TextStrippingConstants.KAFKA_OUTPUT_TOPIC,
                            PluginParameterKeys.NEW_EXTRACTED_DATA_MESSAGE, outputParameters), TextStrippingConstants.INFORM_BOLT_PARALLEL)
                .shuffleGrouping("ExtractText", informStream)
                .shuffleGrouping("StoreNewRepresentation");

        builder.setBolt("ProgressBolt", new ProgressBolt(zkProgressAddress), TextStrippingConstants.PROGRESS_BOLT_PARALLEL)
                .shuffleGrouping("InformBolt");

        return builder.createTopology();
    }
    
    private IRichSpout getSpout()
    {    
        switch(spoutType)
        {
            case FEEDER:
                return new FeederSpout(new StringScheme().getOutputFields());
            case KAFKA:
            default:
                SpoutConfig kafkaConfig = new SpoutConfig(
                    new ZkHosts(TextStrippingConstants.INPUT_ZOOKEEPER), 
                    TextStrippingConstants.KAFKA_INPUT_TOPIC, 
                    TextStrippingConstants.ZOOKEEPER_ROOT, UUID.randomUUID().toString());
                kafkaConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
                return new KafkaSpout(kafkaConfig);
        }
    }
    
    /**
     * @param args the command line arguments
     * @throws backtype.storm.generated.AlreadyAliveException
     * @throws backtype.storm.generated.InvalidTopologyException
     */
    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException 
    {
        TextStrippingTopology textStrippingTopology = new TextStrippingTopology(SpoutType.KAFKA);
        Config config = new Config();
        config.setDebug(true);

        Map<String, String> kafkaMetricsConfig = new HashMap<>();
        kafkaMetricsConfig.put(KafkaMetricsConsumer.KAFKA_BROKER_KEY, TextStrippingConstants.KAFKA_METRICS_BROKER);
        kafkaMetricsConfig.put(KafkaMetricsConsumer.KAFKA_TOPIC_KEY, TextStrippingConstants.KAFKA_METRICS_TOPIC);
        config.registerMetricsConsumer(KafkaMetricsConsumer.class, kafkaMetricsConfig, TextStrippingConstants.METRICS_CONSUMER_PARALLEL);
        
        StormTopology stormTopology = textStrippingTopology.buildTopology();

        if (args != null && args.length > 1) 
        {
            String dockerIp = args[1];
            String name = args[2];
            config.setNumWorkers(1);
            config.setMaxTaskParallelism(1);
            config.put(Config.NIMBUS_THRIFT_PORT, 6627);
            config.put(Config.STORM_ZOOKEEPER_PORT, 2181);
            config.put(Config.NIMBUS_HOST, dockerIp);
            config.put(Config.STORM_ZOOKEEPER_SERVERS, Arrays.asList(args[0]));
            StormSubmitter.submitTopology(name, config, stormTopology);
        } 
        else 
        {
            config.setNumWorkers(1);
            config.setMaxTaskParallelism(1);
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("TextStrippingTopology", config, stormTopology);
            Utils.sleep(6000000);
            cluster.killTopology("TextStrippingTopology");
            cluster.shutdown();
        }
    }
}