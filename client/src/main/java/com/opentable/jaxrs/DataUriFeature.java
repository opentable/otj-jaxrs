package com.opentable.jaxrs;

import java.io.IOException;
import java.net.URI;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;

public class DataUriFeature implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        context.register(DataUriFilter.class);
        return true;
    }

    @Priority(Integer.MAX_VALUE)
    public static class DataUriFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            final URI uri = requestContext.getUri();
            final String scheme = uri.getScheme();
            if (scheme.startsWith("data")) {
                int code = 200;
                final int plusIdx = scheme.indexOf('+');
                if (plusIdx > 0) {
                    code = Integer.parseInt(scheme.substring(plusIdx, scheme.length()));
                }
                requestContext.abortWith(Response.status(code).entity(uri.getSchemeSpecificPart()).build());
            }
        }
    }
}
