package com.opentable.jaxrs.mock;

import java.util.List;
import java.util.function.Function;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP expectations are matched in any order, and may be repeated.
 */
class AnyOrderExpectationFinder implements Function<ClientInvocation, InvocationExpectation> {
    private static final Logger LOG = LoggerFactory.getLogger(AnyOrderExpectationFinder.class);

    private final List<InvocationExpectation> expectations;

    AnyOrderExpectationFinder(List<InvocationExpectation> expectations) {
        this.expectations = expectations;
    }

    @Override
    public InvocationExpectation apply(ClientInvocation t) {
        for (InvocationExpectation e : expectations) {
            if (e.matches(t)) {
                LOG.debug("ClientInvocation '{}' matches expectation '{}'", t, e);
                return e;
            }
            LOG.trace("ClientInvocation '{}' fails to match expectation '{}'", t, e);
        }

        throw new NoExpectationAssertionError("ClientInvocation failed to match any expectation: " + t);
    }
}
