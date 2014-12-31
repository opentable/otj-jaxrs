package com.opentable.jaxrs.mock;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Match a list of expecatations in order, with each expectation being allowed exactly once.
 * Any mismatch causes an {@link AssertionError}.
 */
class InOrderExpectationFinder implements Function<ClientInvocation, InvocationExpectation> {
    private static final Logger LOG = LoggerFactory.getLogger(InOrderExpectationFinder.class);

    private final List<InvocationExpectation> expectations;
    private final AtomicInteger index = new AtomicInteger();

    InOrderExpectationFinder(List<InvocationExpectation> expectations) {
        this.expectations = expectations;
    }

    @Override
    public InvocationExpectation apply(ClientInvocation t) {
        final int nextIndex = index.getAndIncrement();
        if (expectations.size() <= nextIndex) {
            throw new NoExpectationAssertionError("Unexpected invocation #" + nextIndex + " is past end of expectations");
        }
        InvocationExpectation e = expectations.get(nextIndex);
        if (!e.matches(t)) {
            throw new NoExpectationAssertionError("Invocation #" + nextIndex + " doesn't match: " + t);
        }
        LOG.debug("Invocation #{} matches expectation {}", nextIndex, e);
        return e;
    }
}
