package com.opentable.jaxrs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyHttpEngineTest {
    private static final Logger LOG = LoggerFactory.getLogger(JettyHttpEngineTest.class);

    JaxRsClientConfig config = new JaxRsClientConfig() {} ;
    ResteasyClientBuilder builder = new ResteasyClientBuilder();
    Server server = new Server(0);
    Client client;

    @After
    public void stop() throws Exception {
        client.close();
        server.stop();
    }

    private Client client() throws Exception {
        if (!server.isStarted()) {
            server.start();
        }
        if (client == null) {
            builder.httpEngine(new JettyHttpEngine(config));
            client = builder.build();
            builder = null;
        }
        return client;
    }

    @Test
    public void testSimple() throws Exception {
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
                baseRequest.setHandled(true);
                if (baseRequest.getHeader("User-Agent").contains("Apache")) {
                    response.setStatus(503);
                } else if (!"abracadabra".equals(baseRequest.getHeader("Password"))) {
                    response.setStatus(403);
                } else {
                    response.setStatus(200);
                    response.getWriter().println("Success");
                }
            }
        });

        final Response response = client().target(baseUri()).request()
            .header("Password", "abracadabra")
            .get();

        assertEquals(200, response.getStatus());
        assertEquals("Success\n", response.readEntity(String.class));
    }

    @Test
    public void testBigly() throws Exception {
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
                LOG.info("begin handle()");
                baseRequest.setHandled(true);
                response.setStatus(200);
                int read;
                final byte[] data = new byte[1024];
                final ServletInputStream in = request.getInputStream();
                final ServletOutputStream out = response.getOutputStream();
                while ((read = in.read(data)) != -1) {
                    out.write(data, 0, read);
                    LOG.info("copy {}", read);
                }
                LOG.info("end handle()");
            }
        });

        final String valuableData = RandomStringUtils.randomAlphabetic(1024 * 1024 * 2);
        final Response response = client().target(baseUri()).request()
                .post(Entity.text(valuableData));

        assertEquals(200, response.getStatus());
        assertEquals(valuableData, response.readEntity(String.class));
    }

    public URI baseUri() {
        return URI.create("http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort());
    }
}
