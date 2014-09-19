package eu.europeana.cloud.service.uis.security;

import java.net.URISyntaxException;

import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.europeana.cloud.common.exceptions.ProviderDoesNotExistException;
import eu.europeana.cloud.common.model.CloudId;
import eu.europeana.cloud.service.uis.UniqueIdentifierService;
import eu.europeana.cloud.service.uis.exception.CloudIdDoesNotExistException;
import eu.europeana.cloud.service.uis.exception.DatabaseConnectionException;
import eu.europeana.cloud.service.uis.exception.ProviderAlreadyExistsException;
import eu.europeana.cloud.service.uis.exception.RecordDatasetEmptyException;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistException;
import eu.europeana.cloud.service.uis.exception.RecordExistsException;
import eu.europeana.cloud.service.uis.exception.RecordIdDoesNotExistException;
import eu.europeana.cloud.service.uis.rest.UniqueIdentifierResource;

/**
 * UniqueIdentifierResource: Authentication - Authorization tests.
 * 
 * @author manos
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class UisAATest extends AbstractSecurityTest {

    @Autowired
    private UniqueIdentifierService uniqueIdentifierService;
    
	@Autowired
	private UniqueIdentifierResource uisResource;

	private final static String PROVIDER_ID = "Russell_Stringer_Bell";
	private final static String LOCAL_ID = "LOCAL_ID";
	private final static String RECORD_ID = "RECORD_ID";
	private final static String CLOUD_ID = "CLOUD_ID";
	
	/**
	 * Pre-defined users
	 */
	private final static String RANDOM_PERSON = "Cristiano";
	private final static String RANDOM_PASSWORD = "Ronaldo";
	
	private final static String VAN_PERSIE = "Robin_Van_Persie";
	private final static String VAN_PERSIE_PASSWORD = "Feyenoord";
	
	private final static String RONALDO = "Cristiano";
	private final static String RONALD_PASSWORD = "Ronaldo";

	private final static String ADMIN = "admin";
	private final static String ADMIN_PASSWORD = "admin";

    /**
     * Prepare the unit tests
     */
    @Before
    public void prepare() {
    }

	/**
	 * Makes sure these methods can run even if noone is logged in.
	 * No special permissions are required.
	 */
	@Test
    public void testMethodsThatDontNeedAnyAuthentication() throws DatabaseConnectionException, RecordExistsException,
    	ProviderDoesNotExistException, RecordDatasetEmptyException, CloudIdDoesNotExistException, RecordDoesNotExistException {

		uisResource.getCloudId(PROVIDER_ID, RECORD_ID);
		uisResource.getLocalIds(CLOUD_ID);
    }
	
	/**
	 * Makes sure any random person can just call these methods.
	 * No special permissions are required.
	 */
	@Test
    public void shouldBeAbleToCallMethodsThatDontNeedAnyAuthenticationWithSomeRandomPersonLoggedIn() throws DatabaseConnectionException, 
    	RecordExistsException, ProviderDoesNotExistException, RecordDatasetEmptyException, CloudIdDoesNotExistException, RecordDoesNotExistException {

		login(RANDOM_PERSON, RANDOM_PASSWORD);
		uisResource.getCloudId(PROVIDER_ID, RECORD_ID);
		uisResource.getLocalIds(CLOUD_ID);
    }
	
	@Test
    public void shouldBeAbleToCallMethodsThatNeedBasicAuthenticationWithSomeRandomPersonLoggedIn() throws DatabaseConnectionException, RecordExistsException,
    	ProviderDoesNotExistException, RecordDatasetEmptyException, CloudIdDoesNotExistException  {

        CloudId cloudId = new CloudId();
        cloudId.setId(CLOUD_ID);
		
        Mockito.when(uniqueIdentifierService.createCloudId(Mockito.anyString(), Mockito.anyString())).thenReturn(cloudId);
        
		login(RANDOM_PERSON, RANDOM_PASSWORD);
		uisResource.createCloudId(PROVIDER_ID, LOCAL_ID);
    }	

	@Test(expected = AuthenticationCredentialsNotFoundException.class)
    public void shouldThrowExceptionWhenUnknowUserTriesToCreateCloudID() throws DatabaseConnectionException, RecordExistsException,
    	ProviderDoesNotExistException, RecordDatasetEmptyException, CloudIdDoesNotExistException  {

		uisResource.createCloudId(PROVIDER_ID, LOCAL_ID);
    }	

	/**
	 * Makes sure that a random person cannot just delete a cloud id.
	 * Simple authentication test to make sure spring security annotations are in place.
	 */
	@Test(expected = AccessDeniedException.class)
    public void shouldThrowAccessDeniedExceptionWhenRandomPersonTriesToDeleteCloudId() throws DatabaseConnectionException, CloudIdDoesNotExistException,
    	ProviderDoesNotExistException, RecordIdDoesNotExistException  {

		login(RANDOM_PERSON, RANDOM_PASSWORD);
		uisResource.deleteCloudId(CLOUD_ID);
    }
	

	/**
	 * Makes sure the person who created a cloudId has delete permissions as well.
	 */
	@Test
    public void shouldBeAbleToDeleteCloudIdIfHeIsTheOwner() throws ProviderDoesNotExistException, ProviderAlreadyExistsException, 
    	URISyntaxException, DatabaseConnectionException, RecordExistsException, RecordDatasetEmptyException, CloudIdDoesNotExistException, RecordIdDoesNotExistException {
		
        CloudId cloudId = new CloudId();
        cloudId.setId(CLOUD_ID);
		
        Mockito.when(uniqueIdentifierService.createCloudId(Mockito.anyString(), Mockito.anyString())).thenReturn(cloudId);
        
		login(VAN_PERSIE, VAN_PERSIE_PASSWORD);
		uisResource.createCloudId(PROVIDER_ID, LOCAL_ID);
		uisResource.deleteCloudId(CLOUD_ID);
    }
	
	/**
	 * Makes sure Van Persie cannot delete cloud id's that belong to Christiano Ronaldo.
	 */
	@Test(expected = AccessDeniedException.class)
    public void shouldThrowExceptionWhenVanPersieTriesToDeleteRonaldosCloudIds() throws DatabaseConnectionException, RecordExistsException,
    	ProviderDoesNotExistException, RecordDatasetEmptyException, CloudIdDoesNotExistException, URISyntaxException, RecordIdDoesNotExistException  {

        CloudId cloudId = new CloudId();
        cloudId.setId(CLOUD_ID);
		
        Mockito.when(uniqueIdentifierService.createCloudId(Mockito.anyString(), Mockito.anyString())).thenReturn(cloudId);
        
		login(RONALDO, RONALD_PASSWORD);
		uisResource.createCloudId(PROVIDER_ID, LOCAL_ID);
		login(VAN_PERSIE, VAN_PERSIE_PASSWORD);
		uisResource.deleteCloudId(CLOUD_ID);
    }
}