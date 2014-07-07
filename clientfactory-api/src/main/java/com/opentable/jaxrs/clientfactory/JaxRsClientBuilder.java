package com.opentable.jaxrs.clientfactory;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

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
     * Set the timeout for connection attempts
     * @param value - how long to make the timeout
     * @param units - the unit of measurement for the timeout
     * @return this for continued building
     */
    JaxRsClientBuilder connectTimeout(int value, TimeUnit units);

    /**
     * Set the timeout for socket I/O attempts
     * @param value - how long to make the timeout
     * @param units - the unit of measurement for the timeout
     * @return this for continued building
     */
    JaxRsClientBuilder socketTimeout(int value, TimeUnit units);

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
     * Register a filter for performing HTTP Basic Auth using the
     * requested username and password.
     * @return this for continued building
     */
    JaxRsClientBuilder withBasicAuth(String username, String password);

    /**
     * @return the completed JAX-RS client
     */
    Client build();

    /**
     * @return an instance of the currently bound ClientFactory. There should be only one.
     */
    static JaxRsClientBuilder instance() {
        return JaxRsClientBuilderUtil.getInstance();
    }
}
