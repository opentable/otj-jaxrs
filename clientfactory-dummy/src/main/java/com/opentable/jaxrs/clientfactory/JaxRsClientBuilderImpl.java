package com.opentable.jaxrs.clientfactory;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

/**
 * The dummy implementation of the ClientFactory. Don't use this.
 * It's just for testing.
 */
public class JaxRsClientBuilderImpl implements JaxRsClientBuilder
{
    private final String EXPLANATION = "Don't use this package. Use otj-jaxrs-clientfactory-resteasy " +
            "if you are using RESTeasy or otj-jaxrs-clientfactory-jersey if you are using Jersey.";

    @Override
    public JaxRsClientBuilder connectTimeout(int value, TimeUnit units)
    {
        throw new UnsupportedOperationException(EXPLANATION);
    }

    @Override
    public JaxRsClientBuilder socketTimeout(int value, TimeUnit units)
    {
        throw new UnsupportedOperationException(EXPLANATION);
    }

    @Override
    public JaxRsClientBuilder register(Class<?> clazz)
    {
        throw new UnsupportedOperationException(EXPLANATION);
    }

    @Override
    public JaxRsClientBuilder withBasicAuth(String username, String password)
    {
        throw new UnsupportedOperationException(EXPLANATION);
    }

    @Override
    public JaxRsClientBuilder register(Object object)
    {
        throw new UnsupportedOperationException(EXPLANATION);
    }

    @Override
    public Client build()
    {
        throw new UnsupportedOperationException(EXPLANATION);
    }
}
