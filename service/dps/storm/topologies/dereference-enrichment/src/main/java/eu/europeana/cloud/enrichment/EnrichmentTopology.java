package eu.europeana.cloud.enrichment;

import eu.europeana.cloud.enrichment.bolts.EnrichmentBolt;
import eu.europeana.cloud.service.dps.storm.NotificationBolt;
import eu.europeana.cloud.service.dps.storm.NotificationTuple;
import eu.europeana.cloud.service.dps.storm.io.*;
import eu.europeana.cloud.service.dps.storm.spouts.kafka.MCSReaderSpout;
import eu.europeana.cloud.service.dps.storm.topologies.properties.PropertyFileLoader;
import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.grouping.ShuffleGrouping;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Throwables;

import java.util.Properties;

import static eu.europeana.cloud.service.dps.storm.AbstractDpsBolt.NOTIFICATION_STREAM_NAME;
import static eu.europeana.cloud.service.dps.storm.topologies.properties.TopologyPropertyKeys.*;
import static eu.europeana.cloud.service.dps.storm.utils.TopologyHelper.*;
import static java.lang.Integer.parseInt;

/**
 * Created by Tarek on 1/24/2018.
 */
public class EnrichmentTopology {
    private static Properties topologyProperties;
    private static final String TOPOLOGY_PROPERTIES_FILE = "enrichment-topology-config.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichmentTopology.class);

    public EnrichmentTopology(String defaultPropertyFile, String providedPropertyFile) {
        topologyProperties = new Properties();
        PropertyFileLoader.loadPropertyFile(defaultPropertyFile, providedPropertyFile, topologyProperties);

    }

    public StormTopology buildTopology(String enrichmentTopic, String ecloudMcsAddress) {


        MCSReaderSpout mcsReaderSpout = getMcsReaderSpout(topologyProperties, enrichmentTopic, ecloudMcsAddress);

        TopologyBuilder builder = new TopologyBuilder();
        ReadFileBolt retrieveFileBolt = new ReadFileBolt(ecloudMcsAddress);
        WriteRecordBolt writeRecordBolt = new WriteRecordBolt(ecloudMcsAddress);
        RevisionWriterBolt revisionWriterBolt = new RevisionWriterBolt(ecloudMcsAddress);
        EnrichmentBolt enrichmentBolt = new EnrichmentBolt(topologyProperties.getProperty(DEREFERENCE_SERVICE_URL), topologyProperties.getProperty(ENRICHMENT_SERVICE_URL));


        // TOPOLOGY STRUCTURE!
        builder.setSpout(SPOUT, mcsReaderSpout,
                (getAnInt(KAFKA_SPOUT_PARALLEL)))
                .setNumTasks(
                        (getAnInt(KAFKA_SPOUT_NUMBER_OF_TASKS)));

        builder.setBolt(RETRIEVE_FILE_BOLT, retrieveFileBolt,
                (getAnInt(RETRIEVE_FILE_BOLT_PARALLEL)))
                .setNumTasks(
                        (getAnInt(RETRIEVE_FILE_BOLT_NUMBER_OF_TASKS)))
                .customGrouping(SPOUT, new ShuffleGrouping());

        builder.setBolt(ENRICHMENT_BOLT, enrichmentBolt,
                (getAnInt(ENRICHMENT_BOLT_PARALLEL)))
                .setNumTasks(
                        (getAnInt(ENRICHMENT_BOLT_NUMBER_OF_TASKS)))
                .customGrouping(RETRIEVE_FILE_BOLT, new ShuffleGrouping());

        builder.setBolt(WRITE_RECORD_BOLT, writeRecordBolt,
                (getAnInt(WRITE_BOLT_PARALLEL)))
                .setNumTasks(
                        (getAnInt(WRITE_BOLT_NUMBER_OF_TASKS)))
                .customGrouping(ENRICHMENT_BOLT, new ShuffleGrouping());

        builder.setBolt(REVISION_WRITER_BOLT, revisionWriterBolt,
                (getAnInt(REVISION_WRITER_BOLT_PARALLEL)))
                .setNumTasks(
                        (getAnInt(REVISION_WRITER_BOLT_NUMBER_OF_TASKS)))
                .customGrouping(WRITE_RECORD_BOLT, new ShuffleGrouping());

        AddResultToDataSetBolt addResultToDataSetBolt = new AddResultToDataSetBolt(ecloudMcsAddress);
        builder.setBolt(WRITE_TO_DATA_SET_BOLT, addResultToDataSetBolt,
                (getAnInt(ADD_TO_DATASET_BOLT_PARALLEL)))
                .setNumTasks(
                        (getAnInt(ADD_TO_DATASET_BOLT_NUMBER_OF_TASKS)))
                .customGrouping(REVISION_WRITER_BOLT, new ShuffleGrouping());


        builder.setBolt(NOTIFICATION_BOLT, new NotificationBolt(topologyProperties.getProperty(CASSANDRA_HOSTS),
                        getAnInt(CASSANDRA_PORT),
                        topologyProperties.getProperty(CASSANDRA_KEYSPACE_NAME),
                        topologyProperties.getProperty(CASSANDRA_USERNAME),
                        topologyProperties.getProperty(CASSANDRA_SECRET_TOKEN)),
                getAnInt(NOTIFICATION_BOLT_PARALLEL))
                .setNumTasks(
                        (getAnInt(NOTIFICATION_BOLT_NUMBER_OF_TASKS)))
                .fieldsGrouping(SPOUT, NOTIFICATION_STREAM_NAME,
                        new Fields(NotificationTuple.taskIdFieldName))
                .fieldsGrouping(RETRIEVE_FILE_BOLT, NOTIFICATION_STREAM_NAME,
                        new Fields(NotificationTuple.taskIdFieldName))
                .fieldsGrouping(ENRICHMENT_BOLT, NOTIFICATION_STREAM_NAME,
                        new Fields(NotificationTuple.taskIdFieldName))
                .fieldsGrouping(WRITE_RECORD_BOLT, NOTIFICATION_STREAM_NAME,
                        new Fields(NotificationTuple.taskIdFieldName))
                .fieldsGrouping(REVISION_WRITER_BOLT, NOTIFICATION_STREAM_NAME,
                        new Fields(NotificationTuple.taskIdFieldName))
                .fieldsGrouping(WRITE_TO_DATA_SET_BOLT, NOTIFICATION_STREAM_NAME,
                        new Fields(NotificationTuple.taskIdFieldName));


        return builder.createTopology();
    }


    private static int getAnInt(String propertyName) {
        return parseInt(topologyProperties.getProperty(propertyName));
    }

    public static void main(String[] args) {
        try {
            if (args.length <= 1) {

                String providedPropertyFile = "";
                if (args.length == 1) {
                    providedPropertyFile = args[0];
                }

                EnrichmentTopology enrichmentTopology = new EnrichmentTopology(TOPOLOGY_PROPERTIES_FILE, providedPropertyFile);
                String topologyName = topologyProperties.getProperty(TOPOLOGY_NAME);

                // assuming kafka topic == topology name
                String kafkaTopic = topologyName;
                String ecloudMcsAddress = topologyProperties.getProperty(MCS_URL);
                StormTopology stormTopology = enrichmentTopology.buildTopology(
                        kafkaTopic,
                        ecloudMcsAddress);
                Config config = configureTopology(topologyProperties);
                StormSubmitter.submitTopology(topologyName, config, stormTopology);
            }
        } catch (Exception e) {
            LOGGER.error(Throwables.getStackTraceAsString(e));

        }
    }


}

