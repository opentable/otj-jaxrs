package com.opentable.jaxrs.clientfactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.client.Client;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

/**
 * Jersey implementation of JaxRsClientBuilder
 */
public class JaxRsClientBuilderImpl implements JaxRsClientBuilder
{
    private final JerseyClientBuilder clientBuilder = new JerseyClientBuilder();
    private final List<Consumer<JerseyClient>> clientSetup = new ArrayList<>();

    @Override
    public JaxRsClientBuilder connectTimeout(int value, TimeUnit units)
    {
        clientSetup.add(client -> client.property(ClientProperties.CONNECT_TIMEOUT, units.toMillis(value)));
        return this;
    }

    @Override
    public JaxRsClientBuilder socketTimeout(int value, TimeUnit units)
    {
        clientSetup.add(client -> client.property(ClientProperties.READ_TIMEOUT, units.toMillis(value)));
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
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(username, password);
        clientBuilder.register(feature);
        return this;
    }

    @Override
    public Client build()
    {
        JerseyClient client = clientBuilder.build();
        clientSetup.forEach(setup -> setup.accept(client));
        return client;
    }

    private void installStandardFeatures() {
        this.restClient.addFilter(new GZIPContentEncodingFilter(false));
        ClientConfig config = new ClientConfig();
        config.Fea.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

    }
}
