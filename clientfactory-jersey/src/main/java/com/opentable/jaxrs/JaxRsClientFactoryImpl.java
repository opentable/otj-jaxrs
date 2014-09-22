package com.opentable.jaxrs;

import javax.ws.rs.client.ClientBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.message.GZipEncoder;

/**
 * Jersey implementation of InternalClientFactory
 */
public class JaxRsClientFactoryImpl implements InternalClientFactory
{
    @Override
    public ClientBuilder newBuilder(JaxRsClientConfig config) {
        final JerseyClientBuilder builder = new JerseyClientBuilder();
        builder.withConfig(createClientConfig(config));
        configureAuthenticationIfNeeded(builder, config);
        return builder.register(GZipEncoder.class);
    }

    private ClientConfig createClientConfig(JaxRsClientConfig config)
    {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(config.httpClientMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.httpClientDefaultMaxPerRoute());

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, (int) config.connectTimeout().getMillis());
        clientConfig.property(ClientProperties.READ_TIMEOUT, (int) config.socketTimeout().getMillis());
        return clientConfig;
    }

    private static void configureAuthenticationIfNeeded(ClientBuilder builder, JaxRsClientConfig config)
    {
        if (!StringUtils.isEmpty(config.basicAuthUserName()) && !StringUtils.isEmpty(config.basicAuthPassword()))
        {
            HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic(
                    config.basicAuthUserName(), config.basicAuthPassword());
            builder.register(auth);
        }
    }
}
