package com.opentable.jaxrs.mock;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.junit.Test;

public class TestJaxRsMocking {
    @Test
    public void testSimpleMock() throws Exception {
        // Generate the HTTP service
        HttpMockSpec spec = HttpMockSpec.create();
        spec.on().get("/test").respond(Response.ok("Test", MediaType.TEXT_PLAIN));
        spec.on().post("/test").respond(new EchoResponder());
        spec.on().get("/404").respond(Response.status(Status.NOT_FOUND).entity("Not found!"));

        // Create a client that uses the service
        Client client = JaxRsMocking.mockClient(ClientBuilder.newBuilder().register(JacksonJsonProvider.class), spec).build();

        // Give it a whirl
        assertEquals("Test", client.target("http://example.com/test").request().get(String.class));

        SimplePojo sp = client.target("/test").request(MediaType.APPLICATION_JSON).post(Entity.json(new SimplePojo(3, "b")), SimplePojo.class);
        assertEquals(3, sp.a);
        assertEquals("b", sp.b);

        Response r = client.target("/404").request().get();
        assertEquals(Status.NOT_FOUND.getStatusCode(), r.getStatus());
        assertEquals("Not found!", r.readEntity(String.class));
    }

    @Test(expected=NoExpectationAssertionError.class)
    public void testMockNoSpec() throws Exception {
        Client client = JaxRsMocking.mockClient(ClientBuilder.newBuilder(), HttpMockSpec.create()).build();
        client.target("/").request().get();
    }

    public static class SimplePojo {
        public final int a;
        public final String b;

        @JsonCreator
        SimplePojo(@JsonProperty("a") int a, @JsonProperty("b") String b) {
            this.a = a;
            this.b = b;
        }
    }

    private static class EchoResponder implements ResponseGenerator {
        @Override
        public Response respond(ClientInvocation invocation) {
            return Response.ok(invocation.getEntity(), MediaType.APPLICATION_JSON).build();
        }
    }
}
