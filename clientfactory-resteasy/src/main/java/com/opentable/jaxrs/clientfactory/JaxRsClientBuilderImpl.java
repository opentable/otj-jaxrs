package com.opentable.jaxrs.clientfactory;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * The RESTEasy implementation of ClientFactory. Hides RESTEasy specific stuff
 * behind a common facade.
 */
public class JaxRsClientBuilderImpl implements JaxRsClientBuilder
{
    private final ResteasyClientBuilder clientBuilder = getResteasyClientBuilder();

    @Override
    public JaxRsClientBuilder connectTimeout(int value, TimeUnit units)
    {
        clientBuilder.establishConnectionTimeout(value, units);
        return this;
    }

    @Override
    public JaxRsClientBuilder socketTimeout(int value, TimeUnit units)
    {
        clientBuilder.socketTimeout(value, units);
        return this;
    }

    @Override
    public JaxRsClientBuilder register(Object object)
    {
        clientBuilder.register(object);
        return this;
    }

    @Override
    public JaxRsClientBuilder register(Class<?> clazz)
    {
        clientBuilder.register(clazz);
        return this;
    }

    @Override
    public JaxRsClientBuilder withBasicAuth(String username, String password)
    {
        clientBuilder.register(new BasicAuthentication(username, password));
        return this;
    }

    @Override
    public Client build()
    {
        return clientBuilder.build();
    }

    /* package */ ResteasyClientBuilder getResteasyClientBuilder()
    {
        return new ResteasyClientBuilder();
    }

}
