package eu.europeana.cloud.service.coordination.registration;

import java.net.UnknownHostException;

import javax.annotation.PostConstruct;

import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import eu.europeana.cloud.service.coordination.ServiceProperties;
import eu.europeana.cloud.service.coordination.ZookeeperService;

/**
 * Registers services as available in Zookeeper.
 * 
 * ZooKeeper is used for service discovery:
 * 
 * Services are registered on a common znode, and any client can query Zookeeper
 * for a list of available services.
 * 
 * @author emmanouil.koufakis@theeuropeanlibrary.org
 */
public final class ZookeeperServiceAdvertiser implements
		EcloudServiceAdvertiser {

	/** Logging */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ZookeeperServiceAdvertiser.class);

	/**
	 * List of properties that allow a client to connect to this UIS REST Service.
	 */
	private ServiceProperties currentlyAdvertisedServiceProperties;

	/**
	 * Used to serialize the {@link ServiceProperties} before sending them to
	 * Zookeeper.
	 */
	private final InstanceSerializer<ServiceProperties> serializer = new JsonInstanceSerializer<ServiceProperties>(
			ServiceProperties.class);

	/** Service that actually performs the advertisement. */
	private final ServiceDiscovery<ServiceProperties> discovery;

	/**
	 * List of properties that allow a client to connect to some UIS REST Service.
	 */
	private final ServiceProperties serviceProperties;

	ZookeeperServiceAdvertiser(final ZookeeperService zookeeper,
			final ServiceProperties serviceProperties) {

		LOGGER.info("ZookeeperServiceAdvertiser starting...");

		this.serviceProperties = serviceProperties;

		discovery = ServiceDiscoveryBuilder.builder(ServiceProperties.class)
				.basePath(zookeeper.getZookeeperPath())
				.client(zookeeper.getClient()).serializer(serializer).build();

		try {
			discovery.start();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		LOGGER.info("ZookeeperServiceAdvertiser started successfully.");
	}

	/**
	 * Registers this service as available. 
	 * 
	 * Other clients querying Zookeeper for available services will receive this service on their list.
	 * 
	 * @param serviceProperties
	 *            List of properties required to connect to this Service.
	 */
	@Override
	public void startAdvertising(final ServiceProperties serviceProperties) {

		LOGGER.info("ZookeeperServiceAdvertiser startAdvertising...");
		try {
			discovery.registerService(convert(serviceProperties));
		} catch (final Exception e) {
			this.currentlyAdvertisedServiceProperties = null;
			LOGGER.error(e.getMessage());
			throw Throwables.propagate(e);
		}
		LOGGER.info("ZookeeperServiceAdvertiser has advertised the service successfully.");
	}

	@Override
	public void stopAdvertising() {
		try {
			discovery
					.unregisterService(convert(this.currentlyAdvertisedServiceProperties));
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			throw Throwables.propagate(e);
		}
	}

	private static ServiceInstance<ServiceProperties> convert(
			final ServiceProperties p) throws Exception {
		return ServiceInstance.<ServiceProperties> builder()
				.name(p.getServiceName()).payload(p)
				.address(p.getListenAddress()).id(p.getServiceId()).build();
	}

	public void getIpAddress() {

		/**
		 * TODO: address is currently passed through Spring properties
		 * 
		 * https://jira.man.poznan.pl/jira/browse/ECL-194
		 **/
		
		// // TOMCAT 7
		// try {
		//
		// MBeanServer mBeanServer =
		// MBeanServerFactory.findMBeanServer(null).get(0);
		// ObjectName name = new ObjectName("Catalina", "type", "Server");
		// mBeanServer.getAttribute(name, "managedResource");
		// Server server = (Server) mBeanServer.getAttribute(name,
		// "managedResource");
		//
		// LOGGER.info("Server='{}', info='{}', port='{}'", server,
		// server.getInfo(), server.getPort());
		//
		// } catch (Exception e) {
		// LOGGER.error(e.getMessage());
		// }

		// TOMCAT 6
		// Service[] services = server.findServices();
		// for (Service service : services) {
		// for (Connector connector : service.findConnectors()) {
		// ProtocolHandler protocolHandler = connector.getProtocolHandler();
		// if (protocolHandler instanceof Http11Protocol
		// || protocolHandler instanceof Http11AprProtocol
		// || protocolHandler instanceof Http11NioProtocol) {
		// serverPort = connector.getPort();
		// System.out.println("HTTP Port: " + connector.getPort());
		// }
		// }
		// }
	}

	@PostConstruct
	public void postConstruct() throws UnknownHostException {

		LOGGER.info("Server address !TODO! (currently hardcoded in spring properties) == '{}'", serviceProperties.getListenAddress());
		LOGGER.info("ZookeeperService starting advertising process '{}' ...", serviceProperties);

		this.startAdvertising(serviceProperties);

		LOGGER.info("ZookeeperService advertising process successfull.");
	}
}