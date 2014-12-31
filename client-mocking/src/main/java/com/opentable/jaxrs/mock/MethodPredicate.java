package com.opentable.jaxrs.mock;

import java.util.function.Predicate;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

/** Match on HTTP method. */
class MethodPredicate implements Predicate<ClientInvocation> {
    private final String method;

    public MethodPredicate(String method) {
        this.method = method;
    }

    @Override
    public boolean test(ClientInvocation t) {
        return t.getMethod().equalsIgnoreCase(method);
    }

    @Override
    public String toString() {
        return "method '" + method + '\'';
    }
}
