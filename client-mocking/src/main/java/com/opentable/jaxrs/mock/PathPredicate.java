package com.opentable.jaxrs.mock;

import java.util.function.Predicate;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

/** Predicate which matches based on URI path. */
class PathPredicate implements Predicate<ClientInvocation> {

    private final String path;

    PathPredicate(String path) {
        this.path = path;
    }

    @Override
    public boolean test(ClientInvocation t) {
        return t.getUri().getPath().equals(path);
    }

    @Override
    public String toString() {
        return "path '" + path + '\'';
    }
}
