
# Resolving the Component Startup Order
WSO2 introduces a Carbon component startup order resolver implementation that does not need to leverage the existing startup ordering mechanism in OSGi. This new implementation resolves the startup order among multiple components. Further, it notifies a component when all of its dependencies (OSGi services, OSGi bundle etc.) are available. The following sections explain how this solution works, and how it can be implemented for your components.

### Why we need a startup order resolver
WSO2 Carbon Kernel provides an OSGi-based framework for developing enterprise-grade, server-side applications. Transport management, runtime management, centralized logging and deployment engine are some of its core features. When you start a Carbon server, there will be requirements where some components need to wait until other components are initialized (inter-component dependencies). Also, there will be requirements where a component needs to wait until all of its internal services and extensions are available (intra-component dependencies).
The following diagram depicts a scenario with both types of dependencies among Carbon components:
.....
The Microservice Manager and the Transport Manager are Carbon components. Both these components contain one or more related OSGi bundles. In this scenario, the Transport Manager component should be initialized after the Microservice Manager. This is because, the Transport Manager should not open any ports until all the microservices are properly deployed. You can easily handle this via OSGi services:
  1. The Microservice Manager component registers an OSGi service when it is fully initialized. 
  2. The Transport Manager component gets activated soon after that service is available.
Things get complicated when you think about other dependencies of the Microservice Manager and the Transport Manager. Therefore, the initialization of components in the above scenario should happen in the following order:
  1. The Microservice Manager component should only be initialized after all the required microservices are registered.
  2. The Transport Manager component should only be initialized:
        a. after the Microservice Manager component is fully initialized.
        b. after all the transport services are registered from individual bundles during the server startup. For example, the HTTPS transport bundle registers a transport as an OSGi service. There are five such transport bundles shown in the above figure. Therefore, during server startup, you can expect five different OSGi services of type 'transport'. Now, the Transport Manager has to wait until all five OSGi services are available.
       
Transport Manager does not need to wait for OSGi services of type 'transport' that come from transport bundles that get installed after the server starts. Transport Manager only waits for OSGi services of type 'transport' that become available during server startup. Therefore, if someone adds a new transport, or removes an existing transport bundle, the number of transport OSGi services will change. Hence, it is not static, and thereby, you have to calculate the expected number of transport services at runtime. OSGi declarative services do not solve this problem.

### The solution
You need to know the number of OSGi services that are expected by the component. You need to calculate the expected number of services using static resources such as MANIFEST.MF files of OSGi bundles, or any other files inside OSGi bundles. When you know the expected number of OSGi services, you can hold the initialization of the component until all those services are available at runtime.
This may break the dynamism of OSGi because OSGi services can come and go at any time, and thereby depending on a specific number of OSGi services could be tricky. However, WSO2 develops middleware products and those are server-side components. You need to resolve the startup order of WSO2 components. These specific requirements cannot be satisfied using the default mechanisms in OSGi. 
WSO2 recently introduced a framework called WSO2 Microservices Framework for Java (MSF4J), to develop microservices. You can develop and run microservices in a standalone mode and in WSO2 Carbon-based products as OSGi bundles. If you are using Carbon-based products to host your microservices, then you need to register your microservices as OSGi services. Now, when the microservices engine receives a request, engine dispatches it to the correct microservice. Neverthless, microservices engine cannot accept requests until it knows that all the microservices are available as OSGi services.
Otherwise, during server startup, certain microservices will be available while other microservices are yet to be registered as OSGi services. You shouldn’t open ports until all the microservices are available. Microservices engine should notify when all the microservices are registered as OSGi services during the startup. Then only Transport Manager should open ports. All this should happen during the server startup. The Carbon startup order resolver component solves this problem in OSGi.

### About the Carbon startup order resolver
Startup order resolver component is available from WSO2 Carbon kernel 5.0.0 onwards, and its design handles startup ordering complexities in WSO2 Carbon-based products. This is a generic utility, which can resolve startup order of any other component such as microservice engine, Transport Manager etc. 
The startup order resolver component in Carbon ensures that a particular component is not started until all the other components required by that component are fully initialized. To achieve this, the components with dependencies are separated into two categories as shown below.
  1. Startup listener components: Components that need to hold its initialization, until all the required OSGi services or capabilities are available during server startup. For example, the Microservice engine needs to hold it’s initialization until all the microservices from OSGi bundles that are available during server startup are registered as OSGi services. 
  2. OSGi service components: Components that register OSGi services. For example, user management components expose it’s capabilities via a microservice. JMS transport module registers the JMS transport as OSGi services. 
  
#### Defining a startup listener component
A component has to be defined as a listener component, if there are other components that should be initialized before the listener component is initialized. For example, in the above scenario the Transport Manager component should be defined as a listener component, because it requires other transport components to be initialized before it. 
An OSGi listener component is defined as shown below.

The startup order resolver identifies a startup listener component from the following manifest header:
Carbon-Component: startup.listener;
componentName=”transport-mgt”;
requiredService=”org.wso2.carbon.kernel.transports.CarbonTransport”;
startup.listener : Marks this component as a startup listener.
componentName : This is a unique name to identify the component. Each and every startup listener component should have a unique name.
requiredService : A comma separated list of OSGi service keys. These are the OSGi services that the listener component should wait for. That is, the startup listener component should hold it’s initialization until all the services of specified keys are available.
The startup order resolver notifies a startup listener component when all the required services are available. In order to get this notification, the startup listener component should register an OSGi service with the following interface: org.wso2.carbon.kernel.startupresolver.RequiredCapabilityListener.
An implementation of the RequiredCapabilityListener interface is shown below.
public class TransportStartupListener implements RequiredCapabilityListener {
    
    @Override
    public void onAllRequiredCapabilitiesAvailable() {
        // This method is invoked by the startup 
           order resolver when all the required 
           services are available
    }
}

Shown below is how you register the RequiredCapabilityListener implementation given above as an OSGi service in your BundleActivator.
public class TransportBundleActivator implements BundleActivator {
public void start(BundleContext bundleContext)
            throws Exception {

        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("componentName", "transport-mgt");
        bundleContext.registerService(
                TransportStartupListener.class,
                new TransportStartupListener(), properties);
    }

    public void stop(BundleContext bundleContext) throws Exception {

    }
}
The value given for the componentName property should be equal to the componentName property value of the startup listener component that was defined earlier. This is how the startup order resolver maps the RequiredCapabilityListener with corresponding startup listener components (startup.listener).

#### Defining an OSGi service component
A component is required to be defined as an OSGi service component when there are other components depending on the initialization of this component. For example, the Transport Manager component will only be started once the relevant transports are already initialized. Therefore, the transport implementation should be defined as OSGi service components.
Registering a single OSGi service from a component
The startup order resolver identifies an OSGi service component from the following manifest header:
Carbon-Component: osgi.service; 
objectClass=”org.wso2.carbon.kernel.transports.CarbonTransport”
osgi.service: This property marks this component as an OSGi service component.
objectClass: This property indicates the type of OSGi service that will be registered by the component. 
Please note that all components that register OSGi services do not need to include this manifest header. You need to include this header only if there are other components waiting for your OSGi service. For example, the Transport Manager component waits for all the transport OSGi services. If you are developing a transport, you need to put this header into your bundle’s MANIFEST.MF.
Registering multiple OSGI services from a component
When you define an OSGi service component, the component bundle may need to register more than one OSGi service of the same type. This can be done in two ways as explained below.
Registering a known number of services
You can use the serviceCount manifest attribute to specify the number of services that you register from your bundle as shown below. Here you know the exact number of services that your register at development time.
Carbon-Component: osgi.service; 
objectClass=”org.wso2.carbon.kernel.transports.CarbonTransport”; 
serviceCount="4"

Registering an indefinite number of services 
In certain scenarios, you may not know the number of OSGi services you register from your bundle at the time of development. However, during server startup, you can calculate the number of OSGi services that need to be registered. You may obtain this value from a configuration file, from another OSGi service or from any source. In this scenario, you cannot use the serviceCount manifest attribute, since you don’t know the count at development time.
In order to solve this problem, we have introduced an interface called CapabilityProvider from the org.wso2.carbon.kernel.startupresolver package. 
You can register the OSGi service as shown below.
public class HTTPTransportProvider implements CapabilityProvider {
    
    @Override
    public int getCount() {
        return 4;
    }
}Now you need specify the Carbon-Component manifest header as follows.

Now you need to specify the "Carbon-Component" manifest header as shown below.
Carbon-Component: osgi.service; 
objectClass=”org.wso2.carbon.kernel.startupresolver.CapabilityProvider”;
capabilityName="org.wso2.carbon.kernel.transports.CarbonTransport";

Shown below is how you can register the CapabilityProvider implementation shown above as an OSGi service in your BundleActivator.
public class HTTPBundleActivator implements BundleActivator {
    public void start(BundleContext bundleContext)
            throws Exception {

        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("capabilityName",
              "org.wso2.carbon.kernel.transports.CarbonTransport");
        bundleContext.registerService(
                HTTPTransportProvider.class,
                new HTTPTransportProvider(), properties);
    }

    public void stop(BundleContext bundleContext) throws Exception {

    }
}

As explained above, the startup order resolver processes the Carbon-Component manifest headers, and figures out the components that need to be notified when all requirements are satisfied. Similarly, the startup order resolver figures out the expected number of OSGi services for each startup listener component. The startup order resolver listens to OSGi service events, and notifies startup listener components, as and when their requirements are satisfied.

# Adding New Transports
From Carbon 5.0.0 Kernel onwards, we are providing a pluggable interface to add new transports to the existing server. Following are the steps that need to be carried out when adding a new transport to the server.

### Adding a new transport to the Carbon server
Follow the steps given below to implement a new transport.
Implement the CarbonTransport abstract class with the following methods:
protected abstract void start();
protected abstract void stop();
protected abstract void beginMaintenance();
protected abstract void endMaintenance();
Refer the carbon-transport project NettyListener implementation for more details and examples on how to extend the CarbonTransport and 
write your own transport implementation.
Register the implemented server as an OSGi service. For example, If you have extended the CarbonTransport class and implemented JettyCarbonTransport, you need to register the implemented Carbon Transport as follows:
NettyListener nettyCarbonTransport = new NettyListener("netty");
bundleContext.registerService(CarbonTransport.class.getName(), nettyCarbonTransport, null);
Refer registration of NettyListener. You have now registered your transport to the server.

### Registering the transport in the Kernel startup order framework
The Startup Order Resolver component in Kernel allows you to add transports and resolve them statically as well as dynamically. The Transport Manager component in Carbon will only be started once the relevant transports are already initialized. Therefore, the transport implementation should be defined as OSGi service components. Note that your transport can be registered as a single OSGi service or as multiple services. See the instructions on resolving the component startup order.

### Managing transports using OSGi console commands
After registering the new transport, the transports can be managed by the osgi command line. Use ‘help’ to list all the commands available. Following commands are available for the purpose of transport management.
--Transport Management---
 startTransport <transportName> - Start the specified transport with <transportName>.
 stopTransport <transportName> - Stop the specified transport with <transportName>
 startTransports - Start all transports
 stopTransports - Stop all transports
 beginMaintenance - Activate maintenance mode of all transports
 endMaintenance - Deactivate maintenance mode of all transports
 listTransports - List all the available transports
Example: startTransport jetty
