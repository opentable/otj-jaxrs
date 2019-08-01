package com.opentable.jaxrs.util;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

public final class HttpHeadersUtils {
    private HttpHeadersUtils() {
    }

    /**
     * Implementation of {@link HttpHeaders} based on {@link ContainerRequestContext}.
     * @param ctx ContainerRequestContext (jaxrs)
     * @return Httpheaders
     */
    public static HttpHeaders from(final ContainerRequestContext ctx) {
        return new ContainerRequestContextHttpHeaders(ctx);
    }
}
