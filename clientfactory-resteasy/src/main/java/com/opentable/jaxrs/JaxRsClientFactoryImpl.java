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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ProxyBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * The RESTEasy implementation of ClientFactory. Hides RESTEasy specific stuff
 * behind a common facade.
 */
public class JaxRsClientFactoryImpl implements InternalClientFactory
{
    @Override
    public ClientBuilder newBuilder(String clientName, JaxRsClientConfig config) {
        final ResteasyClientBuilder builder = new ResteasyClientBuilder();
        configureHttpEngine(clientName, builder, config);
        configureAuthenticationIfNeeded(clientName, builder, config);
        configureThreadPool(clientName, builder, config);
        return builder;
    }

    @Override
    public <T> T createClientProxy(Class<T> proxyType, WebTarget baseTarget) {
        return ProxyBuilder.builder(proxyType, baseTarget).build();
    }

    public static void configureHttpEngine(String clientName, ResteasyClientBuilder clientBuilder, JaxRsClientConfig config)
    {
        final HttpClient client = new HttpClient();
        try {
            client.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final JettyHttpEngine engine = new JettyHttpEngine(client, config);
        clientBuilder.httpEngine(engine);
    }

    private void configureAuthenticationIfNeeded(String clientName, ResteasyClientBuilder clientBuilder, JaxRsClientConfig config)
    {
        if (!StringUtils.isEmpty(config.getBasicAuthUserName()) && !StringUtils.isEmpty(config.getBasicAuthPassword()))
        {
            final BasicAuthentication auth = new BasicAuthentication(
                    config.getBasicAuthUserName(), config.getBasicAuthPassword());
            clientBuilder.register(auth);
        }
    }

    private void configureThreadPool(String clientName, ResteasyClientBuilder clientBuilder, JaxRsClientConfig config) {
        final ExecutorService executor = new ThreadPoolExecutor(1, 10, 1, TimeUnit.HOURS,
                requestQueue(config.getAsyncQueueLimit()),
                new ThreadFactoryBuilder().setNameFormat(clientName + "-worker-%s").build(),
                new ThreadPoolExecutor.AbortPolicy());
        clientBuilder.executorService(executor);
    }

    private BlockingQueue<Runnable> requestQueue(int size) {
        return size == 0 ? new SynchronousQueue<>() : new ArrayBlockingQueue<>(size);
    }
}
