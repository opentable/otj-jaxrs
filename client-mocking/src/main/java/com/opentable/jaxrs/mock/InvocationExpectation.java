package com.opentable.jaxrs.mock;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An expectation that may match a request and will provide a response if it matches.
 */
class InvocationExpectation {
    private static final Logger LOG = LoggerFactory.getLogger(InvocationExpectation.class);

    private final List<Predicate<ClientInvocation>> predicates;
    private final ResponseGenerator responder;

    InvocationExpectation(List<Predicate<ClientInvocation>> predicates, ResponseGenerator responder) {
        this.responder = responder;
        this.predicates = ImmutableList.copyOf(predicates);
    }

    /**
     * @return true if this expectation matches the given request.
     */
    public boolean matches(ClientInvocation t) {
        for (Predicate<ClientInvocation> p : predicates) {
            if (!p.test(t)) {
                LOG.trace("Expectation {} fails test {} for invocation {}", this, p, t);
                return false;
            }
        }
        LOG.trace("Expectation {} matches invocation {}", this, t);
        return true;
    }

    /**
     * Generate a response for the given request.
     */
    public ClientResponse invoke(ClientInvocation request) {
        if (!matches(request)) throw new AssertionError("Invoke called on a non-matching request" + request);
        return new MockedClientResponse(request.getClientConfiguration(), responder.respond(request));
    }

    @Override
    public String toString() {
        return "Expectation [" + predicates + ']';
    }
}
