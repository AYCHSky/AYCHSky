package eu.europeana.cloud.service.coordination;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * List of properties required to connect to a Service.
 * 
 * Services can register their availability by sending those properties 
 * to a Discovery Service (currently Zookeeper). 
 * 
 * Clients can then query Zookeeper, receive the properties and choose a service to connect to.
 * 
 * @author emmanouil.koufakis@theeuropeanlibrary.org
 */
@JsonRootName("Service")
public final class ServiceProperties {

	@JsonProperty("listenAddress")
	private String listenAddress;
	
	@JsonProperty("serviceName")
	private final String serviceName;
	
	@JsonProperty("datacenterLocation")
	private final String datacenterLocation;

	@JsonCreator
	public ServiceProperties(
			final @JsonProperty("serviceName") String serviceName,
			final @JsonProperty("listenAddress") String listenAddress,
			final @JsonProperty("datacenterLocation") String datacenterLocation) {
		
		this.listenAddress = listenAddress;
		this.serviceName = serviceName;
		this.datacenterLocation = datacenterLocation;
	}
	
	public String getDatacenterLocation() {
		return datacenterLocation;
	}

	public String getListenAddress() {
		return listenAddress;
	}
	
	public String getServiceName() {
		return serviceName;
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
		s.append("DatacenterLocation: ");
		s.append(datacenterLocation);
		return s.toString();
	}
}
