package com.opentable.jaxrs;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;
import org.skife.config.TimeSpan;

/**
 * JAX-RS Client tunables.
 */
interface JaxRsClientConfig
{
    /**
     * Timeout to check out a connection from the connection pool.
     */
    @Config({"jaxrs.client.${clientName}.pool.timeout", "jaxrs.client.default.pool.timeout"})
    @Default("5s")
    TimeSpan connectionPoolTimeout();

    /**
     * Connection pool size.
     */
    @Config({"jaxrs.client.${clientName}.pool.size", "jaxrs.client.default.pool.size"})
    @Default("40")
    int connectionPoolSize();

    /**
     * Timeout to establish initial connection.
     */
    @Config({"jaxrs.client.${clientName}.connect-timeout", "jaxrs.client.default.connect-timeout"})
    @Default("10s")
    TimeSpan connectTimeout();

    /**
     * Socket timeout.
     */
    @Config({"jaxrs.client.${clientName}.socket-timeout", "jaxrs.client.default.socket-timeout"})
    @Default("30s")
    TimeSpan socketTimeout();

    /**
     * Basic auth username.
     */
    @Config({"jaxrs.client.${clientName}.auth.basic.username", "jaxrs.client.default.auth.basic.username"})
    @DefaultNull
    String basicAuthUserName();

    /**
     * Basic auth password.
     */
    @Config({"jaxrs.client.${clientName}.auth.basic.password", "jaxrs.client.default.auth.basic.password"})
    @DefaultNull
    String basicAuthPassword();

    /**
     * Maximum total connections allowed.
     */
    @Config({"jaxrs.client.${clientName}.max-connections", "jaxrs.client.default.max-connections"})
    @Default("100")
    int httpClientMaxTotalConnections();

    /**
     * Maximum connections per-route.
     */
    @Config({"jaxrs.client.${clientName}.max-route-connections", "jaxrs.client.default.max-route-connections"})
    @Default("20")
    int httpClientDefaultMaxPerRoute();

    /**
     * Special hacks for etcd, especially allowing PUTs to 307 redirect.
     */
    @Config({"jaxrs.client.${clientName}.enable-etcd-hacks", "jaxrs.client.default.enable-etcd-hacks"})
    @Default("true")
    boolean isEtcdHacksEnabled();
}
