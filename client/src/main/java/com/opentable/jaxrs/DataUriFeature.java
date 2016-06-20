package com.opentable.jaxrs;

import java.io.IOException;
import java.net.URI;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

class DataUriFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        context.register(DataUriFilter.class);
        return true;
    }

    @Priority(Integer.MAX_VALUE)
    public static class DataUriFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            URI uri = requestContext.getUri();
            if ("data".equals(uri.getScheme())) {
                requestContext.abortWith(Response.ok(uri.getSchemeSpecificPart()).build());
            }
        }
    }
}
