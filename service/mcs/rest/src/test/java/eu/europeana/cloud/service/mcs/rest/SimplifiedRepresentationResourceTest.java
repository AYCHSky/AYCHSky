package eu.europeana.cloud.service.mcs.rest;

import eu.europeana.cloud.client.uis.rest.CloudException;
import eu.europeana.cloud.client.uis.rest.UISClient;
import eu.europeana.cloud.common.exceptions.ProviderDoesNotExistException;
import eu.europeana.cloud.common.model.CloudId;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.response.ErrorInfo;
import eu.europeana.cloud.service.mcs.RecordService;
import eu.europeana.cloud.service.mcs.exception.ProviderNotExistsException;
import eu.europeana.cloud.service.mcs.exception.RecordNotExistsException;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistException;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.core.UriInfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:representationsAccessContext.xml"})
public class SimplifiedRepresentationResourceTest {

    @Autowired
    private SimplifiedRepresentationResource representationResource;

    @Autowired
    private RecordService recordService;

    @Autowired
    private UISClient uisClient;

    private static boolean setUpIsDone = false;

    private static final String PROVIDER_ID = "providerId";
    private static final String CLOUD_ID = "cloudId";
    private static final String NOT_EXISTING_PROVIDER_ID = "notExistingProviderId";
    private static final String LOCAL_ID = "localId";
    private static final String LOCAL_ID_FOR_NOT_EXISTING_RECORD = "localIdForNotExistingRecord";
    private static final String EXISTING_REPRESENTATION_NAME = "existingRepresentationName";
    private static final String RANDOM_REPRESENTATION_NAME = "randomRepresentationName";

    @Before
    public void init() throws CloudException, RepresentationNotExistsException {
        if (setUpIsDone) {
            return;
        }
        setupUisClient();
        setupRecordService();

        setUpIsDone = true;
    }

    @Test(expected = ProviderNotExistsException.class)
    public void exceptionShouldBeThrowForNotExistingProviderId() throws CloudException, RepresentationNotExistsException, ProviderNotExistsException, RecordNotExistsException {
        representationResource.getRepresentation(null, NOT_EXISTING_PROVIDER_ID, "localID", "repName");
    }

    @Test(expected = RecordNotExistsException.class)
    public void exceptionShouldBeThrowForNotExistingCloudId() throws CloudException, RepresentationNotExistsException, ProviderNotExistsException, RecordNotExistsException {
        representationResource.getRepresentation(null, PROVIDER_ID, LOCAL_ID_FOR_NOT_EXISTING_RECORD, "repName");
    }

    @Test(expected = RepresentationNotExistsException.class)
    public void exceptionShouldBeThrowForRecordWithoutNamedRepresentation() throws CloudException, RepresentationNotExistsException, ProviderNotExistsException, RecordNotExistsException {
        representationResource.getRepresentation(null, PROVIDER_ID, LOCAL_ID, RANDOM_REPRESENTATION_NAME);
    }

    @Test
    public void properRepresentationShouldBeReturned() throws CloudException, RepresentationNotExistsException, ProviderNotExistsException, RecordNotExistsException {
        UriInfo info = Mockito.mock(UriInfo.class);
        Mockito.when(info.getBaseUriBuilder()).thenReturn(new JerseyUriBuilder());
        //
        Representation rep = representationResource.getRepresentation(info, PROVIDER_ID, LOCAL_ID, EXISTING_REPRESENTATION_NAME);
        //
        Assert.assertNotNull(rep);
        assertThat(rep.getCloudId(), is(CLOUD_ID));
        assertThat(rep.getRepresentationName(), is(EXISTING_REPRESENTATION_NAME));
    }

    /////////////
    //
    /////////////
    private void setupUisClient() throws CloudException {
        //
        CloudId cid = new CloudId();
        cid.setId(CLOUD_ID);
        //


        Mockito.when(uisClient.getCloudId(Mockito.eq(NOT_EXISTING_PROVIDER_ID), Mockito.anyString())).thenThrow(new CloudException("", new ProviderDoesNotExistException(new ErrorInfo())));
        Mockito.when(uisClient.getCloudId(PROVIDER_ID, LOCAL_ID_FOR_NOT_EXISTING_RECORD)).thenThrow(new CloudException("", new RecordDoesNotExistException(new ErrorInfo())));
        Mockito.when(uisClient.getCloudId(PROVIDER_ID, LOCAL_ID)).thenReturn(cid);
    }

    private void setupRecordService() throws RepresentationNotExistsException {
        Representation rep = new Representation(CLOUD_ID, EXISTING_REPRESENTATION_NAME, "sampleVersion", null, null, PROVIDER_ID, null,null, true, null);

        Mockito.when(recordService.getRepresentation(CLOUD_ID, EXISTING_REPRESENTATION_NAME)).thenReturn(rep);
        Mockito.when(recordService.getRepresentation(CLOUD_ID, RANDOM_REPRESENTATION_NAME)).thenThrow(RepresentationNotExistsException.class);
    }
}