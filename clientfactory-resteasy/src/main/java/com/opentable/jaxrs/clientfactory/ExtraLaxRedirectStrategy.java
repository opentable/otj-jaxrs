package com.opentable.jaxrs.clientfactory;

import org.apache.http.impl.client.DefaultRedirectStrategy;

class ExtraLaxRedirectStrategy extends DefaultRedirectStrategy {
    @Override
    protected boolean isRedirectable(String method) {
        return true;
    }
}
