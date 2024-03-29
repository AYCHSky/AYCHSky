package eu.europeana.cloud.mcs.driver;

import eu.europeana.cloud.common.filter.ECloudBasicAuthFilter;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.common.response.ErrorInfo;
import eu.europeana.cloud.common.utils.Tags;
import eu.europeana.cloud.common.web.ParamConstants;
import eu.europeana.cloud.mcs.driver.exception.DriverException;
import eu.europeana.cloud.service.mcs.exception.MCSException;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;

import static eu.europeana.cloud.common.web.ParamConstants.*;

/**
 * Created by Tarek on 8/2/2016.
 */
public class RevisionServiceClient extends MCSClient {

    private final Client client;
    private static final Logger logger = LoggerFactory.getLogger(RevisionServiceClient.class);
    private static final String revisionPathWithTag = "records/{" + P_CLOUDID + "}/representations/{"
            + P_REPRESENTATIONNAME + "}/versions/{" + P_VER + "}/revisions/{" + P_REVISION_NAME + "}/revisionProvider/{" + P_REVISION_PROVIDER_ID + "}/tag/{" + P_TAG + "}";

    private static final String revisionPath = "records/{" + P_CLOUDID + "}/representations/{"
            + P_REPRESENTATIONNAME + "}/versions/{" + P_VER + "}/revisions";

    private static final String revisionPathWithMultipleTags = "records/{" + P_CLOUDID + "}/representations/{"
            + P_REPRESENTATIONNAME + "}/versions/{" + P_VER + "}/revisions/{" + P_REVISION_NAME + "}/revisionProvider/{" + P_REVISION_PROVIDER_ID + "}/tags";

    private static final String REMOVE_REVISION_PATH = "records/{" + P_CLOUDID + "}/representations/{"
            + P_REPRESENTATIONNAME + "}/versions/{" + P_VER + "}/revisions/{" + P_REVISION_NAME + "}/revisionProvider/{" + P_REVISION_PROVIDER_ID + "}";


    /**
     * Constructs a RevisionServiceClient
     *
     * @param baseUrl url of the MCS Rest Service
     */

    public RevisionServiceClient(String baseUrl) {
        this(baseUrl, DEFAULT_CONNECT_TIMEOUT_IN_MILLIS, DEFAULT_READ_TIMEOUT_IN_MILLIS);
    }

    public RevisionServiceClient(String baseUrl, final int connectTimeoutInMillis, final int readTimeoutInMillis) {
        super(baseUrl);
        client = JerseyClientBuilder.newClient().register(MultiPartFeature.class);
        this.client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeoutInMillis);
        this.client.property(ClientProperties.READ_TIMEOUT, readTimeoutInMillis);
    }

    /**
     * Creates instance of RevisionServiceClient. Same as {@link #RevisionServiceClient(String)}
     * but includes username and password to perform authenticated requests.
     *
     * @param baseUrl URL of the MCS Rest Service
     */

    public RevisionServiceClient(String baseUrl, final String username, final String password) {
        this(baseUrl, username, password, DEFAULT_CONNECT_TIMEOUT_IN_MILLIS, DEFAULT_READ_TIMEOUT_IN_MILLIS);
    }


    public RevisionServiceClient(String baseUrl, final String username, final String password, final int connectTimeoutInMillis, final int readTimeoutInMillis) {
        super(baseUrl);
        client = JerseyClientBuilder.newClient()
                .register(MultiPartFeature.class)
                .register(HttpAuthenticationFeature.basicBuilder().credentials(username, password).build());
        this.client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeoutInMillis);
        this.client.property(ClientProperties.READ_TIMEOUT, readTimeoutInMillis);
    }

    public void useAuthorizationHeader(final String headerValue) {
        client.register(new ECloudBasicAuthFilter(headerValue));
    }

    /**
     * add a revision
     *
     * @param cloudId            id of uploaded revision.
     * @param representationName representation name of uploaded revision.
     * @param version            a specific version of the representation.
     * @param revisionName       the name of revision
     * @param revisionProviderId revision provider id
     * @param tag                flag tag
     * @return URI to specific revision with specific tag inside a version.
     * @throws DriverException call to service has not succeeded because of server side error.
     * @throws MCSException    on unexpected situations.
     */
    public URI addRevision(String cloudId, String representationName, String version, String revisionName, String revisionProviderId, String tag)
            throws
            DriverException, MCSException {
        WebTarget target = client.target(baseUrl).path(revisionPathWithTag).resolveTemplate(P_CLOUDID, cloudId)
                .resolveTemplate(P_REPRESENTATIONNAME, representationName)
                .resolveTemplate(P_VER, version).resolveTemplate(P_REVISION_NAME, revisionName).resolveTemplate(P_REVISION_PROVIDER_ID, revisionProviderId).resolveTemplate(P_TAG, tag);

        Invocation.Builder request = target.request();
        Response response = null;
        try {
            response = request.post(null);
            return handleAddRevisionResponse(response);
        } finally {
            closeResponse(response);
        }
    }

    /**
     * add a revision
     *
     * @param cloudId            id of uploaded revision.
     * @param representationName representation name of uploaded revision.
     * @param version            a specific version of the representation.
     * @param revision           revision
     * @return URI to revisions inside a version.
     * @throws RepresentationNotExistsException when representation does not exist in specified version.
     * @throws DriverException                  call to service has not succeeded because of server side error.
     * @throws MCSException                     on unexpected situations.
     */
    public URI addRevision(String cloudId, String representationName, String version, Revision revision)
            throws
            DriverException, MCSException {
        WebTarget target = client.target(baseUrl).path(revisionPath).resolveTemplate(P_CLOUDID, cloudId)
                .resolveTemplate(P_REPRESENTATIONNAME, representationName)
                .resolveTemplate(ParamConstants.P_VER, version);
        Invocation.Builder request = target.request();
        Response response = null;
        try {
            response = request.accept(MediaType.APPLICATION_JSON).post(Entity.json(revision));
            return handleAddRevisionResponse(response);
        } finally {
            closeResponse(response);
        }
    }


    /**
     * add a revision
     *
     * @param cloudId            id of uploaded revision.
     * @param representationName representation name of uploaded revision.
     * @param version            a specific version of the representation.
     * @param revision           revision
     * @param key                key of header request
     * @param value              value of header request
     * @return URI to revisions inside a version.
     * @throws RepresentationNotExistsException when representation does not exist in specified version.
     * @throws DriverException                  call to service has not succeeded because of server side error.
     * @throws MCSException                     on unexpected situations.
     */
    public URI addRevision(String cloudId, String representationName, String version, Revision revision, String key, String value)
            throws
            DriverException, MCSException {
        WebTarget target = client.target(baseUrl).path(revisionPath).resolveTemplate(P_CLOUDID, cloudId)
                .resolveTemplate(P_REPRESENTATIONNAME, representationName)
                .resolveTemplate(ParamConstants.P_VER, version);
        Invocation.Builder request = target.request().header(key, value);
        Response response = null;
        try {
            response = request.accept(MediaType.APPLICATION_JSON).post(Entity.json(revision));
            return handleAddRevisionResponse(response);
        } finally {
            closeResponse(response);
        }
    }

    private URI handleAddRevisionResponse(Response response) throws MCSException {
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            return response.getLocation();
        } else {
            ErrorInfo errorInfo = response.readEntity(ErrorInfo.class);
            throw MCSExceptionProvider.generateException(errorInfo);
        }
    }


    /**
     * add a revision
     *
     * @param cloudId            cloud id of the record (required).
     * @param representationName schema of representation (required).
     * @param version            a specific version of the representation(required).
     * @param revisionName       the name of revision (required).
     * @param revisionProviderId revision provider id (required).
     * @param tags               set of tags (acceptance,published,deleted)
     * @return URI to a revision tags inside a version.
     * @throws RepresentationNotExistsException when representation does not exist in specified version.
     * @throws DriverException                  call to service has not succeeded because of server side error.
     * @throws MCSException                     on unexpected situations.
     */
    public URI addRevision(String cloudId, String representationName, String version, String revisionName, String revisionProviderId, Set<Tags> tags)
            throws
            DriverException, MCSException {
        WebTarget target = client.target(baseUrl).path(revisionPathWithMultipleTags).resolveTemplate(P_CLOUDID, cloudId)
                .resolveTemplate(P_REPRESENTATIONNAME, representationName)
                .resolveTemplate(ParamConstants.P_VER, version).resolveTemplate(ParamConstants.P_REVISION_NAME, revisionName).resolveTemplate(P_REVISION_PROVIDER_ID, revisionProviderId);
        Form tagsForm = new Form();
        for (Tags tag : tags) {
            tagsForm.param(F_TAGS, tag.getTag());
        }
        Invocation.Builder request = target.request();
        Response response = null;
        try {
            response = request.post(Entity.form(tagsForm));
            return handleAddRevisionResponse(response);
        } finally {
            closeResponse(response);
        }
    }


    /**
     * Remove a revision
     *
     * @param cloudId            cloud Id
     * @param representationName representation name
     * @param version            representation version
     * @param revisionName       revision name
     * @param revisionProvider   revision provider
     * @param revisionTimestamp  revision timestamp
     * @throws RepresentationNotExistsException
     */

    public void deleteRevision(String cloudId, String representationName, String version, String revisionName, String revisionProvider, String revisionTimestamp)
            throws DriverException, MCSException {
        WebTarget target = client.target(baseUrl).path(REMOVE_REVISION_PATH)
                .resolveTemplate(P_CLOUDID, cloudId)
                .resolveTemplate(P_REPRESENTATIONNAME, representationName)
                .resolveTemplate(ParamConstants.P_VER, version)
                .resolveTemplate(P_REVISION_NAME, revisionName)
                .resolveTemplate(P_REVISION_PROVIDER_ID, revisionProvider).queryParam(F_REVISION_TIMESTAMP, revisionTimestamp);

        Invocation.Builder request = target.request();
        Response response = null;
        try {
            response = request.delete();
            if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
                ErrorInfo errorInfo = response.readEntity(ErrorInfo.class);
                throw MCSExceptionProvider.generateException(errorInfo);
            }
        } finally {
            closeResponse(response);
        }

    }


    private void closeResponse(Response response) {
        if (response != null) {
            response.close();
        }
    }

    public void close() {
        client.close();
    }

    @Override
    protected void finalize() throws Throwable {
        client.close();
    }

}
