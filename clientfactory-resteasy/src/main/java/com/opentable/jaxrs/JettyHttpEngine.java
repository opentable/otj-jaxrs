package com.opentable.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.Callback;
import org.jboss.resteasy.client.jaxrs.AsyncClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

class JettyHttpEngine implements AsyncClientHttpEngine {
    private static final Logger LOG = LoggerFactory.getLogger(JettyHttpEngine.class);
    private static final InvocationCallback<ClientResponse> SYNC = new InvocationCallback<ClientResponse>() {
        @Override
        public void completed(ClientResponse response) {
        }

        @Override
        public void failed(Throwable throwable) {
        }
    };

    private final HttpClient client;
//    private final ByteBufferPool bufs;
//    private final JaxRsClientConfig config;
    private final FiberExecutorScheduler fibers;

    JettyHttpEngine(JaxRsClientConfig config) {
        client = new HttpClient();
        client.setIdleTimeout(config.getIdleTimeout().toMillis());
        try {
            client.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        this.config = config;
        fibers = new FiberExecutorScheduler("jetty-jaxrs", client.getExecutor());
    }

    @Override
    public SSLContext getSslContext() {
        return client.getSslContextFactory().getSslContext();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientResponse invoke(ClientInvocation invocation) {
        Future<ClientResponse> future = submit(invocation, true, SYNC, result -> result);
        try {
            return future.get(10, TimeUnit.MINUTES); // TODO: configurable?
        } catch (InterruptedException e) {
            future.cancel(true);
            throw clientException(e, null);
        } catch (ExecutionException | TimeoutException e) {
            throw clientException(e.getCause(), null); // NOPMD
        }
    }

    @SuppressWarnings("resource")
    @Override
    public <T> Future<T> submit(ClientInvocation invocation, boolean buffered, InvocationCallback<T> callback, ResultExtractor<T> extractor) {
        final Request request = client.newRequest(invocation.getUri());
        final CompletableFuture<T> future = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                final boolean cancelled = super.cancel(mayInterruptIfRunning);
                if (mayInterruptIfRunning && cancelled) {
                    request.abort(new CancellationException());
                }
                return cancelled;
            }
        };

        request.method(invocation.getMethod());
        invocation.getHeaders().asMap().forEach((h, vs) -> vs.forEach(v -> request.header(h, v)));

        if (invocation.getEntity() != null) {
            // todo: don't buffer?
            LOG.debug("writeRequestBody {}", request);
            try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
                invocation.writeRequestBody(stream);
                LOG.debug("done writeRequestBody {}", request);
                request.content(new BytesContentProvider(stream.toByteArray()));
            } catch (IOException e) {
                throw new ProcessingException(e);
            }
        }

        LOG.debug("send {} as {}", invocation, request);
        request.send(new Response.Listener.Adapter() {
            private ClientResponse cr;
            private JettyResponseStream stream = new JettyResponseStream();

            @Override
            public void onHeaders(Response response) {
                LOG.debug("{} headers {}", request, response);
                responseReady(response);
                if (!buffered) {
                    future.complete((T) cr); // XXX
                }
            }

            @Override
            @Suspendable
            public void onContent(Response response, ByteBuffer content, Callback callback) {
                LOG.debug("content {} {}", request, content.remaining());
                try {
                    stream.offer(content, callback);
                } catch (SuspendExecution e) {
                    throw new AssertionError(e);
                }
            }

            @Override
            public void onFailure(Response response, Throwable failure) {
                LOG.debug("failure {}", request, failure);
                future.completeExceptionally(failure);
                callback.failed(failure);
            }

            @Override
            public void onSuccess(Response response) {
                LOG.debug("success {} {}", request, response);
            }

            @Override
            public void onComplete(Result result) {
                LOG.debug("complete {} {}", request, result);
                try {
                    stream.close();
                } catch (IOException e) {
                    LOG.warn("while closing stream", e);
                }
            }

            private void responseReady(Response response) {
                LOG.debug("responseReady {} {}", request, response);
                if (cr != null) {
                    return;
                }
                cr = new JettyClientResponse(invocation.getClientConfiguration(), stream, () -> future.cancel(true));
                cr.setProperties(invocation.getMutableProperties());
                cr.setStatus(response.getStatus());
                cr.setHeaders(extract(response.getHeaders()));
                new Fiber<>(fibers, () -> {
                    LOG.debug("{} begin extractResult", request);
                    final T t = extractor.extractResult(cr);
                    LOG.debug("{} end extractResult", request);
                    future.complete(t);
                    callback.completed(t);
                }).start();
            }
        });
        return future;
    }

    @Override
    public void close() {
        try {
            client.stop();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to close JettyHttpEngine", e);
        }
    }

    MultivaluedMap<String, String> extract(HttpFields headers) {
        final MultivaluedMap<String, String> extracted = new MultivaluedHashMap<>();
        headers.forEach(h -> extracted.add(h.getName(), h.getValue()));
        return extracted;
    }

    private static RuntimeException clientException(Throwable ex, javax.ws.rs.core.Response clientResponse) {
        RuntimeException ret;
        if (ex == null) {
            ret = new ProcessingException(new NullPointerException()); // NOPMD
        }
        else if (ex instanceof WebApplicationException) {
            ret = (WebApplicationException) ex;
        }
        else if (ex instanceof ProcessingException) {
            ret = (ProcessingException) ex;
        }
        else if (clientResponse != null) {
            ret = new ResponseProcessingException(clientResponse, ex);
        }
        else {
            ret = new ProcessingException(ex);
        }
        return ret;
    }
}
