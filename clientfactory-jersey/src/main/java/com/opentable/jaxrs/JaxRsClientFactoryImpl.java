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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.http.impl.client.IdleConnectionEvictor;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.message.GZipEncoder;
import org.springframework.context.ApplicationContext;

/**
 * Jersey implementation of InternalClientFactory
 */
public class JaxRsClientFactoryImpl implements InternalClientFactory
{
    public JaxRsClientFactoryImpl(ApplicationContext ctx) {
    }

    @Override
    public ClientBuilder newBuilder(String clientName, JaxRsClientConfig config, Collection<JaxRsFeatureGroup> featureGroups) {
        final JerseyClientBuilder builder = new JerseyClientBuilder();
        builder.withConfig(createClientConfig(config));
        return builder.register(GZipEncoder.class);
    }

    @Override
    public <T> T createClientProxy(Class<T> proxyType, WebTarget baseTarget) {
        throw new UnsupportedOperationException("Jersey support for this feature is TODO");
    }

    private ClientConfig createClientConfig(JaxRsClientConfig config)
    {
        final EvictablePoolingHttpClientConnectionManager connectionManager = new EvictablePoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(config.getConnectionPoolSize());
        connectionManager.setDefaultMaxPerRoute(config.getHttpClientDefaultMaxPerRoute());

        connectionManager.ice = new IdleConnectionEvictor(connectionManager, config.getIdleTimeout().toMillis(), TimeUnit.MILLISECONDS);
        connectionManager.ice.start();

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, (int) config.getConnectTimeout().toMillis());
        clientConfig.property(ClientProperties.READ_TIMEOUT, (int) config.getSocketTimeout().toMillis());
        return clientConfig;
    }

    private static class EvictablePoolingHttpClientConnectionManager extends PoolingHttpClientConnectionManager {
        private volatile IdleConnectionEvictor ice;

        @Override
        public void shutdown() {
            ice.shutdown();
            super.shutdown();
        }
    }
}
