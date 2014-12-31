package com.opentable.jaxrs.mock;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

public class JaxRsMocking {
    /**
     * Customize a ClientBuilder to use the given HTTP service specification to respond instead of
     * making actual HTTP requests.
     */
    public static ClientBuilder mockClient(ClientBuilder builder, HttpMockSpec spec) {
        return ((ResteasyClientBuilder) builder).httpEngine(new MockHttpEngine(spec));
    }
}
