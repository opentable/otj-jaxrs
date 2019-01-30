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

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.MonitoredPoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.springframework.context.ApplicationContext;

/**
 * The RESTEasy implementation of ClientFactory. Hides RESTEasy specific stuff
 * behind a common facade.
 */
public class JaxRsClientFactoryImpl implements InternalClientFactory
{


    public JaxRsClientFactoryImpl(ApplicationContext ctx) {
       /* unused */
    }

    public ClientBuilder newBuilder(String clientName, JaxRsClientConfig config) {
        final ExecutorService executorService = configureThreadPool(clientName, config);
        final ResteasyClientBuilderImpl builder = new ResteasyClientBuilderImpl();
        builder.asyncExecutor(executorService, true);
        configureHttpEngine(clientName, builder, config);
        configureAuthenticationIfNeeded(clientName, builder, config);
        return builder;
    }

    @Override
    public ClientBuilder newBuilder(final String clientName, final JaxRsClientConfig config, final Collection<JaxRsFeatureGroup> featureGroups) {
        return newBuilder(clientName, config);
    }

    @Override
    public <T> T createClientProxy(Class<T> proxyType, WebTarget baseTarget) {
        return ProxyBuilder.builder(proxyType, baseTarget).build();
    }

    public static void configureHttpEngine(String clientName, ResteasyClientBuilder clientBuilder, JaxRsClientConfig config)
    {
        final HttpClient client = prepareHttpClientBuilder(clientName, config).build();
        final ApacheHttpClient43Engine engine = new HackedApacheHttpClient4Engine(config, client);
        clientBuilder.httpEngine(engine);
    }

    public static HttpClientBuilder prepareHttpClientBuilder(String clientName, JaxRsClientConfig config)
    {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        /*
        if (config.isEtcdHacksEnabled()) {
            builder
                .setRedirectStrategy(new ExtraLaxRedirectStrategy())
                .addInterceptorFirst(new SwallowHeaderInterceptor(HttpHeaders.CONTENT_LENGTH));
        }
        */
        if (!config.isCookieHandlingEnabled()) {
            builder.disableCookieManagement();
        }
        final MonitoredPoolingHttpClientConnectionManager connectionManager = new MonitoredPoolingHttpClientConnectionManager(clientName);

        connectionManager.setCheckoutWarnTime(Duration.ofMillis(config.getConnectionPoolWarnTime().toMillis()));
        connectionManager.setMaxTotal(config.getConnectionPoolSize());
        connectionManager.setDefaultMaxPerRoute(config.getHttpClientDefaultMaxPerRoute());

        return builder
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout((int) config.getSocketTimeout().toMillis())
                        .build())
                .setDefaultRequestConfig(customRequestConfig(config, RequestConfig.custom()))
                .setConnectionManager(connectionManager)
                .evictIdleConnections(config.getIdleTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    private static RequestConfig customRequestConfig(JaxRsClientConfig config, RequestConfig.Builder base) {
        base.setRedirectsEnabled(true);
        if (config != null) {
            base.setConnectionRequestTimeout((int) config.getConnectionPoolTimeout().toMillis())
                .setConnectTimeout((int) config.getConnectTimeout().toMillis())
                .setSocketTimeout((int) config.getSocketTimeout().toMillis());
        }
        return base.build();
    }

    private void configureAuthenticationIfNeeded(String clientName, ResteasyClientBuilder clientBuilder, JaxRsClientConfig config)
    {
        /*
        This used to be supported in config....
        if (!StringUtils.isEmpty(config.getBasicAuthUserName()) && !StringUtils.isEmpty(config.getBasicAuthPassword()))
        {
            final BasicAuthentication auth = new BasicAuthentication(
                    config.getBasicAuthUserName(), config.getBasicAuthPassword());
            clientBuilder.register(auth);
        }
        */
    }

    private ExecutorService configureThreadPool(String clientName, JaxRsClientConfig config) {
        final ExecutorService executor = new ThreadPoolExecutor(1, calculateThreads(config.getExecutorThreads()), 1, TimeUnit.HOURS,
                requestQueue(config.getAsyncQueueLimit()),
                new ThreadFactoryBuilder().setNameFormat(clientName + "-worker-%s").build(),
                new ThreadPoolExecutor.AbortPolicy());
        return executor;
       // clientBuilder.asyncExecutor(executor, true);
    }

    private int calculateThreads(final int executorThreads) {
        if (executorThreads > 0) {
            return  executorThreads;
        }
        // For current standard 8 core machines this is 10 regardless.
        // On Java 10, you might get less than 8 core reported, but it will still size as if it's 8
        // Beyond 8 core you MIGHT undersize if running on Docker, but otherwise should be fine.
        return Math.max(10, (Runtime.getRuntime().availableProcessors() + 2 ));
    }

    private BlockingQueue<Runnable> requestQueue(int size) {
        return size == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(size);
    }

    private static class HackedApacheHttpClient4Engine extends ApacheHttpClient43Engine {
        private final JaxRsClientConfig config;

        HackedApacheHttpClient4Engine(JaxRsClientConfig config, HttpClient client) {
            super(client);
            this.config = config;
        }

        @Override
        protected HttpRequestBase createHttpMethod(String url, String restVerb) {
            final HttpRequestBase result = super.createHttpMethod(url, restVerb);
            final Builder base = result.getConfig() == null ? RequestConfig.custom() : RequestConfig.copy(result.getConfig());
            result.setConfig(customRequestConfig(config, base));
            return result;
        }

        @Override
        protected void loadHttpMethod(ClientInvocation request, HttpRequestBase httpMethod) throws Exception {
            super.loadHttpMethod(request, httpMethod);
            if (Boolean.FALSE.equals(request.getMutableProperties().get(JaxRsClientProperties.FOLLOW_REDIRECTS))) {
                httpMethod.setConfig(RequestConfig.copy(httpMethod.getConfig()).setRedirectsEnabled(false).build());
            }
            request.property(JaxRsClientProperties.ACTUAL_REQUEST, httpMethod);
        }
    }

    private static class SwallowHeaderInterceptor implements HttpRequestInterceptor {
        private final String[] headers;

        SwallowHeaderInterceptor(String... headers) {
            this.headers = headers == null ? new String[0] :  headers.clone();
        }

        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            for (String header : headers) {
                request.removeHeaders(header);
            }
        }
    }
}
