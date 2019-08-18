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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.Test;

@SuppressWarnings("restriction")
public class ResteasyClientBuilderTest {
    private static final String BAD_URI = "http://example.invalid";
    private final JaxRsClientConfig config = new JaxRsClientConfig() {};

    @Test
    public void testNoRedirect() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 1);
        try {
            server.createContext("/", new RedirectHandler());
            server.start();

            final InetSocketAddress addr = server.getAddress();
            Client client = new JaxRsClientFactoryImpl(null).newBuilder("test", config).build();
            try {
                Response r = client.target("http://" + addr.getHostString() + ":" + addr.getPort()).request()
                        .property(JaxRsClientProperties.FOLLOW_REDIRECTS, false)
                        .get();
                assertEquals(301, r.getStatus());
                assertEquals(BAD_URI, r.getHeaderString(HttpHeaders.LOCATION));
            } finally {
                client.close();
            }
        } finally {
            server.stop(0);
        }
    }

    private static class RedirectHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange h) throws IOException {
            h.getResponseHeaders().add(HttpHeaders.LOCATION, BAD_URI);
            h.sendResponseHeaders(301, -1);
            h.close();
        }
    }
}
