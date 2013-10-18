package eu.europeana.cloud.service.mcs.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * ProviderNotExistsException
 */
public class ProviderNotExistsException extends WebApplicationException {

    public ProviderNotExistsException() {
        super(Response.Status.NOT_FOUND);
    }
}