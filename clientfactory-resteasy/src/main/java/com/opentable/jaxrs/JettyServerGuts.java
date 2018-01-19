package com.opentable.jaxrs;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.Scheduler;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;

final class JettyServerGuts {
    private JettyServerGuts() { }
    public static BiConsumer<ResteasyClientBuilder, HttpClient> extract(ApplicationContext ctx) {
        final Server server;
        try {
            server = ((JettyWebServer) ctx.getBean(WebServer.class)).getServer();
        } catch (NoSuchBeanDefinitionException e) {
            return (rcb, hc) -> {};
        }
        return (rcb, hc) -> {
            final Executor executor = server.getBean(Executor.class);
            rcb.executorService((ExecutorService)executor);
            hc.setByteBufferPool(server.getBean(ByteBufferPool.class));
            hc.setExecutor(executor);
            hc.setScheduler(server.getBean(Scheduler.class));
        };
    }
}
