package com.opentable.jaxrs.clientfactory;

import java.util.Properties;

import javax.ws.rs.client.Client;

import org.skife.config.ConfigurationObjectFactory;

/**
 * A factory for producing JAX-RS clients with low-level socket-handling
 * details set in a implementation-independent way.
 * <p>
 * Doesn't tie you to any particular implementation at compile time.
 * </p>
 */
public interface JaxRsClientBuilder
{

    /**
     * Register a component instance for use in the client
     * @param object - the component instance to use
     * @return this for continued building
     */
    JaxRsClientBuilder register(Object object);

    /**
     * Register a component class for use in the client
     * @param clazz - the component class to use
     * @return this for continued building
     */
    JaxRsClientBuilder register(Class<?> clazz);

    /**
     * Apply a configuration to this client builder, to apply settings in the HttpClient.
     * @return this for continued building
     */
    JaxRsClientBuilder withConfiguration(JaxRsClientConfig config);

    /**
     * Apply a configuration to this client builder, to apply settings in the HttpClient.
     * @return this for continued building
     */
    default JaxRsClientBuilder withConfiguration(Properties configProperties) {
        final JaxRsClientConfig config = new ConfigurationObjectFactory(configProperties).build(JaxRsClientConfig.class);
        return withConfiguration(config);
    }

    /**
     * @return the completed JAX-RS client
     */
    Client build();

    /**
     * @return an instance of the currently bound ClientFactory. There should be only one.
     */
    static JaxRsClientBuilder newInstance() {
        return JaxRsClientBuilderUtil.newInstance();
    }
}
