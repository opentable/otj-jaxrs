/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    public ClientBuilder newBuilder(String clientName, JaxRsClientConfig config) {
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
