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

import org.eclipse.jetty.util.JavaVersion;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.JettyResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
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
    private static final String KEY_DISABLE =  "ot.client.jaxrs.disableTLS13";
    private static final Logger LOG = LoggerFactory.getLogger(JaxRsClientFactoryImpl.class);
    private static final String DEF_VALUE = "true";
    private Supplier<TlsProvider> provider;
    private final List<Consumer<SslContextFactory>> factoryCustomizers = new ArrayList<>();

    public JaxRsClientFactoryImpl(ApplicationContext ctx) {
//        if (ctx != null && ClassUtils.isPresent("org.eclipse.jetty.server.Server", null)) {
            // TODO: figure out how to wire up thread pool
//        }
        provider = () -> null;
        boolean disableTLS = false;
        if (ctx != null) {
            provider = () -> {
                try {
                    return ctx.getBean(TlsProvider.class);
                } catch (NoSuchBeanDefinitionException e) {
                    return null;
                }
            };
           disableTLS = Boolean.parseBoolean(ctx.getEnvironment().getProperty(KEY_DISABLE, DEF_VALUE))
                   && (JavaVersion.VERSION.getMajor() == 11);
        }
        if (disableTLS) {
            this.factoryCustomizers.add(sslContextFactory ->  {
                LOG.info("Disabling TLS 1.3");
                sslContextFactory.setExcludeProtocols("TLSv1.3");
            });
        }
    }

    @Override
    public ClientBuilder newBuilder(String clientName, JaxRsClientConfig config, Collection<JaxRsFeatureGroup> featureGroups) {
        final ResteasyClientBuilder builder = new JettyResteasyClientBuilder(clientName, config,
                featureGroups.contains(StandardFeatureGroup.PLATFORM_INTERNAL) ? provider.get() : null, factoryCustomizers);
        configureThreadPool(clientName, builder, config);
        return builder;
    }

    @Override
    public <T> T createClientProxy(Class<T> proxyType, WebTarget baseTarget) {
        return ProxyBuilder.builder(proxyType, baseTarget).build();
    }

    private void configureThreadPool(String clientName, ResteasyClientBuilder clientBuilder, JaxRsClientConfig config) {
        final int threads = CalculateThreads.calculateThreads(config.getExecutorThreads(), clientName);
        // We used a fixed thread pool here instead of a QueuedThreadPool (which would lead to lower memory)
        // Primarily because resteasy wants an ExecutorService not an Executor
        // Reexamine in future
        // See https://docs.google.com/spreadsheets/d/179upsXNJv_xMWYHZLY2e0456bxoBYbOHW7ORV3m-CxE/edit#gid=0
        final ExecutorService executor = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.HOURS,
                requestQueue(config.getAsyncQueueLimit()),
                new ThreadFactoryBuilder().setNameFormat(clientName + "-worker-%s").build(),
                new ThreadPoolExecutor.AbortPolicy());
        clientBuilder.executorService(executor);
    }

    private BlockingQueue<Runnable> requestQueue(int size) {
        return size == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(size);
    }
}
