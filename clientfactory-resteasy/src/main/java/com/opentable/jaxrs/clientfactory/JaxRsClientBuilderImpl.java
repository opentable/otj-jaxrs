package com.opentable.jaxrs.clientfactory;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * The RESTEasy implementation of ClientFactory. Hides RESTEasy specific stuff
 * behind a common facade.
 */
public class JaxRsClientBuilderImpl implements JaxRsClientBuilder
{
    private final ResteasyClientBuilder clientBuilder = getResteasyClientBuilder();

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
    public JaxRsClientBuilder withConfiguration(JaxRsClientConfig config)
    {
        configureHttpEngine(config);
        configureAuthenticationIfNeeded(config);
        clientBuilder.establishConnectionTimeout(config.connectTimeoutMillis(), TimeUnit.MILLISECONDS);
        clientBuilder.socketTimeout(config.socketTimeoutMillis(), TimeUnit.MILLISECONDS);
        return this;
    }

    @Override
    public Client build()
    {
        return clientBuilder.build();
    }

    /** package-public, used in test only */
    ResteasyClientBuilder getResteasyClientBuilder()
    {
        return new ResteasyClientBuilder();
    }

    private void configureHttpEngine(JaxRsClientConfig config)
    {
        final HttpClient client = HttpClientBuilder.create()
                .setMaxConnTotal(config.httpClientMaxTotalConnections())
                .setMaxConnPerRoute(config.httpClientDefaultMaxPerRoute())
                .build();
        final ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(client);
        clientBuilder.httpEngine(engine);
    }

    private void configureAuthenticationIfNeeded(JaxRsClientConfig config)
    {
        if (!StringUtils.isEmpty(config.basicAuthUserName()) && !StringUtils.isEmpty(config.basicAuthPassword()))
        {
            final BasicAuthentication auth = new BasicAuthentication(
                    config.basicAuthUserName(), config.basicAuthPassword());
            clientBuilder.register(auth);
        }
    }
}
