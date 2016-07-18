package eu.europeana.cloud.service.dps.storm.utils;

import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Tarek on 11/13/2015.
 */
public class TaskTupleUtilityTest {
    StormTaskTuple stormTaskTuple;
    static final String MIME_TYPE = "text/xml";

    @Before
    public void init() {
        stormTaskTuple = new StormTaskTuple();
    }


    @Test
    public void parameterIsProvidedTest() {
        stormTaskTuple.addParameter(PluginParameterKeys.MIME_TYPE, MIME_TYPE);
        assertTrue(TaskTupleUtility.isProvidedAsParameter(stormTaskTuple, PluginParameterKeys.MIME_TYPE));
    }

    @Test
    public void parameterIsNotProvidedTest() {
        assertFalse(TaskTupleUtility.isProvidedAsParameter(stormTaskTuple, PluginParameterKeys.MIME_TYPE));
    }


    @Test
    public void getDefaultValueTest() {
        assertEquals(TaskTupleUtility.getParameterFromTuple(stormTaskTuple, PluginParameterKeys.MIME_TYPE), PluginParameterKeys.PLUGIN_PARAMETERS.get(PluginParameterKeys.MIME_TYPE));
    }


    @Test
    public void getProvidedValueTest() {
        stormTaskTuple.addParameter(PluginParameterKeys.MIME_TYPE, MIME_TYPE);
        assertEquals(TaskTupleUtility.getParameterFromTuple(stormTaskTuple, PluginParameterKeys.MIME_TYPE), MIME_TYPE);

    }


}
