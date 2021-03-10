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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.opentable.http.common.HttpClientCommonConfiguration;
import com.opentable.http.common.ImmutableHttpClientCommonConfiguration;

/**
 * The RESTEasy implementation of ClientFactory. Hides RESTEasy specific stuff
 * behind a common facade.  Uses Jetty-Client.
 */
public class JaxRsClientFactoryImpl implements InternalClientFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(JaxRsClientFactoryImpl.class);
    private Supplier<TlsProvider> provider;

    public JaxRsClientFactoryImpl(ApplicationContext ctx) {
//        if (ctx != null && ClassUtils.isPresent("org.eclipse.jetty.server.Server", null)) {
            // TODO: figure out how to wire up thread pool
//        }
        provider = () -> null;
        if (ctx != null) {
            provider = () -> {
                try {
                    return ctx.getBean(TlsProvider.class);
                } catch (NoSuchBeanDefinitionException e) {
                    return null;
                }
            };
        }

    }

    @Override
    public ClientBuilder newBuilder(String clientName, JaxRsClientConfig config, Collection<JaxRsFeatureGroup> featureGroups) {
        HttpClientCommonConfiguration httpClientCommonConfiguration =
                ImmutableHttpClientCommonConfiguration.builder()
                .connectTimeout(config.getConnectTimeout())
                .idleTimeout(config.getIdleTimeout())
                .isRemoveUserAgent(config.isRemoveUserAgent())
                .userAgent(config.getUserAgent())
                // Deviation 1: JAXRS uses this formula. Might as well keep it
                .maxConnectionsPerHost(Math.max(64, config.getHttpClientDefaultMaxPerRoute()))
                .followRedirect(false)
                .isDisableCompression(config.getDisableCompression())
                .isDisableTLS13(config.isDisableTLS13())
                .isLimitConnectionPool(config.isTuneConnectionPool())
                .maxUsages(config.getMexUsages())
                .threadPoolName(clientName)
                .threadsPerPool(config.getExecutorThreads())
                 // Deviation 2: For complicated reasons (RestEasy api) we can't use the default QTP.
                .executor(configureThreadPool(clientName, config))
                 // none of the others wire this up
                .proxyHost(Optional.ofNullable(config.getProxyHost()))
                .proxyPort(config.getProxyPort())
                .isCookieHandlingEnabled(config.isCookieHandlingEnabled())
                .build();



        final List<Consumer<SslContextFactory>> sslFactoryContextCustomizers = getSSlFactoryContextCustomizers(config, featureGroups);
        return new JettyResteasyClientBuilder(true, httpClientCommonConfiguration, sslFactoryContextCustomizers)
                .connectTimeout(config.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .executorService((ExecutorService) httpClientCommonConfiguration.getExecutor().get());
    }

    // Note: Most of this is probably obsolete and related to Steven setting up rotating SSL certs.
    private List<Consumer<SslContextFactory>> getSSlFactoryContextCustomizers(final JaxRsClientConfig config, final Collection<JaxRsFeatureGroup> featureGroups) {
        final List<Consumer<SslContextFactory>> factoryCustomizers = new ArrayList<>();
        final TlsProvider tlsProvider = featureGroups.contains(StandardFeatureGroup.PLATFORM_INTERNAL) ? provider.get() : null;
        if (tlsProvider != null) {
            addProviderCustomizer(tlsProvider, factoryCustomizers);
        }
        return factoryCustomizers;
    }

    @Override
    public <T> T createClientProxy(Class<T> proxyType, WebTarget baseTarget) {
        return ProxyBuilder.builder(proxyType, baseTarget).build();
    }

    private ExecutorService configureThreadPool(String clientName, JaxRsClientConfig config) {
        final int threads = new com.opentable.http.common.
                CalculateThreads().calculateThreads(config.getExecutorThreads(), clientName);
        // We used a fixed thread pool here instead of a QueuedThreadPool (which would lead to lower memory)
        // Primarily because resteasy wants an ExecutorService not an Executor
        // Reexamine in future
        // See https://docs.google.com/spreadsheets/d/179upsXNJv_xMWYHZLY2e0456bxoBYbOHW7ORV3m-CxE/edit#gid=0
        return new ThreadPoolExecutor(threads, threads, 1, TimeUnit.HOURS,
                requestQueue(config.getAsyncQueueLimit()),
                new ThreadFactoryBuilder().setNameFormat(clientName + "-worker-%s").build(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private BlockingQueue<Runnable> requestQueue(int size) {
        return size == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(size);
    }

    private void addProviderCustomizer(final TlsProvider tlsProvider, final List<Consumer<SslContextFactory>> factoryCustomizers) {
        factoryCustomizers.add(sslContextFactory -> tlsProvider.init((ts, ks) -> {
            try {
                sslContextFactory.reload(f -> {
                    f.setValidateCerts(true);
                    f.setValidatePeerCerts(true);
                    f.setKeyStorePassword("");
                    f.setKeyStore(ks);
                    f.setTrustStorePassword("");
                    f.setTrustStore(ts);
                });
                LOG.debug("Rotated client {} TLS keys to {}", sslContextFactory, ks);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
