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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.ConnectionPool;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.RoundRobinConnectionPool;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

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
        final List<Consumer<SslContextFactory>> sslFactoryContextCustomizers = getSSlFactoryContextCustomizers(config, featureGroups);
        final List<Consumer<HttpClient>> httpClientCustomizers = getHttpClientCustomizers(clientName, config);
        return new JettyResteasyClientBuilder(true, httpClientCustomizers, sslFactoryContextCustomizers)
                .connectTimeout(config.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .executorService(configureThreadPool(clientName, config));
    }

    private List<Consumer<HttpClient>> getHttpClientCustomizers(final String clientName, final JaxRsClientConfig config) {
        final List<Consumer<HttpClient>> httpClientCustomizers = new ArrayList<>();
        if (config.getIdleTimeout() != null) {
            httpClientCustomizers.add(hc -> hc.setIdleTimeout(config.getIdleTimeout().toMillis()));
        }
        httpClientCustomizers.add(hc -> hc.setRemoveIdleDestinations(true));
        if (config.getUserAgent() != null) {
            final HttpField userAgentFieldValue = config.isRemoveUserAgent()
                    ? null :
                    new HttpField(HttpHeader.USER_AGENT, config.getUserAgent());
            final String userAgentChangedMessage = config.isRemoveUserAgent()
                    ? "Removing User-Agent"
                    : String.format("Setting User-Agent for the HTTP client %s to %s", clientName, config.getUserAgent());
            httpClientCustomizers.add(hc -> {
                LOG.info(userAgentChangedMessage);
                hc.setUserAgentField(userAgentFieldValue);
            });
        }
        if (config.isCookieHandlingEnabled()) {
            httpClientCustomizers.add(hc -> hc.setCookieStore(new HttpCookieStore()));
        }
        if(StringUtils.isNotBlank(config.getProxyHost()) && config.getProxyPort() != 0) {
            httpClientCustomizers.add(hc -> hc.getProxyConfiguration().getProxies().add(new HttpProxy(config.getProxyHost(), config.getProxyPort())));
        }
        final int maxPerDestination = Math.max(64, config.getHttpClientDefaultMaxPerRoute());
        httpClientCustomizers.add(hc -> hc.setMaxConnectionsPerDestination(maxPerDestination));

        if (config.isTuneConnectionPool()) {
            httpClientCustomizers.add(hc -> hc.getTransport().setConnectionPoolFactory(httpDestination -> {
                RoundRobinConnectionPool c =  new RoundRobinConnectionPool(httpDestination, maxPerDestination, httpDestination);
                c.setMaxUsageCount(config.getMexUsages());
                return c;
            }));
        }
        return httpClientCustomizers;
    }

    private List<Consumer<SslContextFactory>> getSSlFactoryContextCustomizers(final JaxRsClientConfig config, final Collection<JaxRsFeatureGroup> featureGroups) {
        final List<Consumer<SslContextFactory>> factoryCustomizers = new ArrayList<>();
        if (config.isDisableTLS13()) {
            factoryCustomizers.add(sslContextFactory ->  {
                LOG.info("Disabling TLS 1.3");
                sslContextFactory.setExcludeProtocols("TLSv1.3");
            });
        }
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
        final int threads = CalculateThreads.calculateThreads(config.getExecutorThreads(), clientName);
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
