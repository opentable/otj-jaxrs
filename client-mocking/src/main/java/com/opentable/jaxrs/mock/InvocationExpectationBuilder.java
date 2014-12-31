package com.opentable.jaxrs.mock;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.ws.rs.core.Response.ResponseBuilder;

import com.google.common.collect.Lists;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

/**
 * Builder class for HTTP expectations.
 */
public class InvocationExpectationBuilder {
    private final Consumer<InvocationExpectation> onBuild;
    private final List<Predicate<ClientInvocation>> predicates = Lists.newArrayList();
    private volatile boolean built = false;

    InvocationExpectationBuilder(Consumer<InvocationExpectation> onBuild) {
        this.onBuild = onBuild;
    }

    /** Request is GET and matches the given path. */
    public InvocationExpectationBuilder get(String path) {
        return method("GET").path(path);
    }
    /** Request is POST and matches the given path. */
    public InvocationExpectationBuilder post(String path) {
        return method("POST").path(path);
    }
    /** Request is PUT and matches the given path. */
    public InvocationExpectationBuilder put(String path) {
        return method("PUT").path(path);
    }
    /** Request is DELETE and matches the given path. */
    public InvocationExpectationBuilder delete(String path) {
        return method("DELETE").path(path);
    }

    /** Request has the given method. */
    public InvocationExpectationBuilder method(String method) {
        predicates.add(new MethodPredicate(method));
        return this;
    }

    /** Request has the given path. */
    public InvocationExpectationBuilder path(String path) {
        predicates.add(new PathPredicate(path));
        return this;
    }

    /** Provide the response for a matched request. */
    public void respond(ResponseGenerator responseGenerator) {
        if (built) {
            throw new IllegalStateException("Already built");
        }
        built = true;

        onBuild.accept(new InvocationExpectation(predicates, responseGenerator));
    }

    /** Provide the response for a matched request. */
    public void respond(ResponseBuilder responseBuilder) {
        respond(inv -> responseBuilder.build());
    }
}
