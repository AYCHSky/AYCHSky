package eu.europeana.cloud.service.dps.storm;

import eu.europeana.cloud.cassandra.CassandraConnectionProviderSingleton;
import eu.europeana.cloud.common.model.dps.AttributeStatistics;
import eu.europeana.cloud.common.model.dps.NodeStatistics;
import eu.europeana.cloud.service.dps.storm.utils.CassandraAttributeStatisticsDAO;
import eu.europeana.cloud.service.dps.storm.utils.CassandraTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CassandraAttributeStatisticsDAOTest extends CassandraTestBase {
    private static final long TASK_ID = 1;

    private static final String NODE_1_XPATH = "/root/node1";

    private static final String ATTRIBUTE_1_NAME = "attribute1";

    private static final String ATTRIBUTE_1_VALUE = "value1";

    private static final String ATTRIBUTE_2_NAME = "attribute2";

    private static final String ATTRIBUTE_2_VALUE = "value1";

    private CassandraAttributeStatisticsDAO attributeStatisticsDAO;

    @Before
    public void setUp() {
        attributeStatisticsDAO = CassandraAttributeStatisticsDAO.getInstance(CassandraConnectionProviderSingleton.getCassandraConnectionProvider(HOST, PORT, KEYSPACE, "", ""));
    }

    @Test
    public void testShouldProperlyStoreAttributeStatistics() {
        // given
        Set<AttributeStatistics> toStore = prepareAttributeStatistics();

        // when
        attributeStatisticsDAO.insertAttributeStatistics(TASK_ID, NODE_1_XPATH, toStore);

        // then
        Set<AttributeStatistics> retrieved = attributeStatisticsDAO.getAttribtueStatistics(TASK_ID, NODE_1_XPATH);
        Assert.assertEquals(retrieved.size(), toStore.size());
        for (AttributeStatistics stats : toStore) {
            Assert.assertTrue(retrieved.contains(stats));
        }
    }

    @Test
    public void testShouldProperlyUpdateAttributeStatistics() {
        // given
        Set<AttributeStatistics> toStore = prepareAttributeStatistics();

        // when
        attributeStatisticsDAO.insertAttributeStatistics(TASK_ID, NODE_1_XPATH, toStore);
        attributeStatisticsDAO.insertAttributeStatistics(TASK_ID, NODE_1_XPATH, toStore);

        // then
        Set<AttributeStatistics> retrieved = attributeStatisticsDAO.getAttribtueStatistics(TASK_ID, NODE_1_XPATH);
        Assert.assertEquals(retrieved.size(), toStore.size());
        for (AttributeStatistics stats : toStore) {
            Assert.assertTrue(retrieved.contains(stats));
        }
        for (AttributeStatistics stats : retrieved) {
            Assert.assertEquals(2, stats.getOccurrence());
        }
    }

    private Set<AttributeStatistics> prepareAttributeStatistics() {
        Set<AttributeStatistics> statistics = new HashSet<>();
        statistics.add(new AttributeStatistics(ATTRIBUTE_1_NAME, ATTRIBUTE_1_VALUE));
        statistics.add(new AttributeStatistics(ATTRIBUTE_2_NAME, ATTRIBUTE_2_VALUE));
        return statistics;
    }
}
