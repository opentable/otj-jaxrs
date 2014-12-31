package com.opentable.jaxrs.mock;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;

/** Provide Responses for given requests. */
public interface ResponseGenerator {
    /** Generate a single response. */
    Response respond(ClientInvocation invocation);
}
