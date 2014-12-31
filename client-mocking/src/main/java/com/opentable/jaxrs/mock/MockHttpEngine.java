package com.opentable.jaxrs.mock;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import com.google.common.base.Throwables;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;

/**
 * {@link ClientHttpEngine} implementation which services requests
 * according to a {@link HttpMockSpec}.
 */
class MockHttpEngine implements ClientHttpEngine {
    private volatile boolean closed = false;
    private final HttpMockSpec spec;

    MockHttpEngine(HttpMockSpec spec) {
        this.spec = spec;
    }

    @Override
    public SSLContext getSslContext() {
        try {
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        };
    }

    @Override
    public ClientResponse invoke(ClientInvocation request) {
        if (closed) throw new IllegalStateException("closed");
        return spec.invoke(request);
    }

    @Override
    public void close() {
        closed = true;
    }
}
