package com.opentable.jaxrs;

import org.apache.http.impl.client.DefaultRedirectStrategy;

class ExtraLaxRedirectStrategy extends DefaultRedirectStrategy {
    @Override
    protected boolean isRedirectable(String method) {
        return true;
    }
}
