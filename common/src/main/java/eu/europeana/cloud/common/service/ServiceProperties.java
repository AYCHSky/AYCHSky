package eu.europeana.cloud.common.service;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * List of properties required to connect to a Service.
 * 
 * Services can register their availability by sending those properties 
 * to a Discovery Service (currently Zookeeper). 
 * 
 * Clients can then query Zookeeper, receive the properties and choose a service to connect to
 * (and can optionally load-balance using 
 * {@link #databaseLoad} and {@link #serviceLoad}).
 * 
 * @author emmanouil.koufakis@theeuropeanlibrary.org
 */
@JsonRootName("Service")
public final class ServiceProperties {

	@JsonProperty("serviceId")
	private final String serviceId;

	@JsonProperty("listenAddress")
	private String listenAddress;
	
	@JsonProperty("serviceName")
	private final String serviceName;

	@JsonProperty("databaseLoad")
	private final int databaseLoad;
	
	@JsonProperty("serviceLoad")
	private final int serviceLoad;
	
	@JsonCreator
	public ServiceProperties(final @JsonProperty("serviceId") String serviceId,
			final @JsonProperty("serviceName") String serviceName,
			final @JsonProperty("listenAddress") String listenAddress) {
		
		this.serviceId = serviceId;
		this.listenAddress = listenAddress;
		this.serviceName = serviceName;
		this.databaseLoad = 0;
		this.serviceLoad = 0;
	}
	
	public ServiceProperties(final String serviceId,
			final String serviceName,
			final String listenAddress,
			final int databaseLoad,
			final int serviceLoad) {
		
		this.serviceId = serviceId;
		this.listenAddress = listenAddress;
		this.serviceName = serviceName;
		this.databaseLoad = databaseLoad;
		this.serviceLoad = serviceLoad;
	}

	public String getServiceId() {
		return serviceId;
	}

	public String getListenAddress() {
		return listenAddress;
	}
	
	public String getServiceName() {
		return serviceName;
	}

	public int getServiceLoad() {
		return serviceLoad;
	}
	
	public int getDatabaseLoad() {
		return databaseLoad;
	}
	
	public void setListenAddress(final String listenAddress) {
		this.listenAddress = listenAddress;
	}
	
	@Override
	public String toString() {
		
		final StringBuffer s = new StringBuffer();
		s.append("Service Name: ");
		s.append(serviceName);
		s.append(", ");
		s.append("ListenAddress: ");
		s.append(listenAddress);
		s.append(", ");
		s.append("ServiceLoad: ");
		s.append(serviceLoad);
		s.append(", ");
		s.append("DatabaseLoad: ");
		s.append(databaseLoad);
		return s.toString();
	}
}
