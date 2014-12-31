package com.opentable.jaxrs.mock;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;

/**
 * A mocked HTTP service.  The service is a set of expectations and response generators.
 * When a request is made, the request is matched against the list of expectations.  (By default,
 * the first matched expectation wins).  The expectation then provides a response to the request.
 */
public class HttpMockSpec {

    private final List<InvocationExpectation> expectations = new CopyOnWriteArrayList<>();
    private Function<ClientInvocation, InvocationExpectation> expectationFinder = new AnyOrderExpectationFinder(expectations);

    /**
     * Create a new, initially empty, HTTP service.
     */
    public static HttpMockSpec create() {
        return new HttpMockSpec();
    }

    /**
     * Require expectations to be matched in order and once only.
     */
    public HttpMockSpec inOrder() {
        expectationFinder = new InOrderExpectationFinder(expectations);
        return this;
    }

    /**
     * Begin adding a new expectation.
     */
    public InvocationExpectationBuilder on() {
        return new InvocationExpectationBuilder(expectations::add);
    }

    /**
     * Respond to a request.
     */
    ClientResponse invoke(ClientInvocation request) {
        return expectationFinder.apply(request).invoke(request);
    }
}
