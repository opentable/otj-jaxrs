package com.opentable.jaxrs.clientfactory;

import java.util.Optional;

import javax.ws.rs.client.Client;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.GZipEncoder;

import com.opentable.logging.Log;

/**
 * Jersey implementation of JaxRsClientBuilder
 */
public class JaxRsClientBuilderImpl implements JaxRsClientBuilder
{
    private static final Log LOG = Log.findLog();

    private final JerseyClientBuilder clientBuilder = new JerseyClientBuilder();

    private Optional<ClientConfig> clientConfig = Optional.empty();

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
        this.clientConfig = Optional.of(createClientConfig(config));
        configureAuthenticationIfNeeded(config);
        return this;
    }

    @Override
    public Client build()
    {
        LOG.info("creating Jersey JAX-RS client");

        return clientConfig // if there's a config
                .map(clientBuilder::withConfig)  // then use client with config
                .orElse(clientBuilder)           // otherwise go without
                .register(JacksonFeature.class)
                .register(GZipEncoder.class)
                .build();
    }

    private ClientConfig createClientConfig(JaxRsClientConfig config)
    {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(config.httpClientMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.httpClientDefaultMaxPerRoute());

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, config.connectTimeout().getMillis());
        clientConfig.property(ClientProperties.READ_TIMEOUT, config.socketTimeout().getMillis());
        return clientConfig;
    }

    private void configureAuthenticationIfNeeded(JaxRsClientConfig config)
    {
        if (!StringUtils.isEmpty(config.basicAuthUserName()) && !StringUtils.isEmpty(config.basicAuthPassword()))
        {
            HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic(
                    config.basicAuthUserName(), config.basicAuthPassword());
            clientBuilder.register(auth);
        }
    }
}
