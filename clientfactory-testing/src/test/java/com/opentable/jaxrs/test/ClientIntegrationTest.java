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
package com.opentable.jaxrs.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sun.net.httpserver.HttpServer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.config.Config;
import com.opentable.jaxrs.JaxRsClientFactory;
import com.opentable.jaxrs.JaxRsClientModule;
import com.opentable.jaxrs.StandardFeatureGroup;
import com.opentable.lifecycle.guice.LifecycleModule;

public class ClientIntegrationTest {

    public static final int SERVER_PORT = 8910;
    private static final Logger LOG = LoggerFactory.getLogger(ClientIntegrationTest.class);
    private Injector injector;
    private JaxRsClientFactory factory;

    private InetSocketAddress address;
    private HttpServer httpServer;


    @Before
    public void setup() throws IOException {
        injector = Guice.createInjector(
                new JaxRsClientModule("test", StandardFeatureGroup.PUBLIC),
                new LifecycleModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Config.class).toInstance(Config.getFixedConfig(
                                "jaxrs.client.default.http.max-route-connections", "10"
                        ));
                    }
                }
        );
        factory = injector.getInstance(JaxRsClientFactory.class);
        address = new InetSocketAddress(SERVER_PORT);
        LOG.debug("creating server at address {}", address);
        httpServer = HttpServer.create(address, 0);
        final byte[] response = "Hello!\n".getBytes();
        httpServer.createContext("/", exchange -> {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        httpServer.start();
    }

    @After
    public void after() {
        httpServer.stop(0);
    }

    @Test(expected=Exception.class)
    public void exhaustConnectionPoolWithoutClose() throws InterruptedException {
        final Client client = factory.newClient("test", StandardFeatureGroup.PUBLIC);
        final URI uri = UriBuilder.fromUri("http://"+address.getHostName()).port(SERVER_PORT).build();

        for (int i = 0; i < 100; i++) {
            LOG.trace("trying connection number {} for {}", i, uri);
            final Response response = client.target(uri).request().get();

            assertEquals("status should be 200", 200, response.getStatus());
        }
    }

    @Test
    public void closedConnectionPoolNotExhaused() throws InterruptedException {
        final Client client = factory.newClient("test", StandardFeatureGroup.PUBLIC);

        final URI uri = UriBuilder.fromUri("http://"+address.getHostName()).port(SERVER_PORT).build();
        for (int i = 0; i < 100; i++) {
            LOG.trace("trying connection number {} for {}", i, uri);
            final Response response = client.target(uri).request().get();

            response.close();
            assertEquals("status should be 200", 200, response.getStatus());
        }
    }

    @Test
    public void entityReadClosesToo() throws InterruptedException {
        final Client client = factory.newClient("test", StandardFeatureGroup.PUBLIC);

        final URI uri = UriBuilder.fromUri("http://"+address.getHostName()).port(SERVER_PORT).build();
        for (int i = 0; i < 100; i++) {
            LOG.trace("trying connection number {} for {}", i, uri);
            final Response response = client.target(uri).request().get();
            final String result = response.readEntity(String.class);
            assertEquals("status should be 200", 200, response.getStatus());
        }
    }
}
