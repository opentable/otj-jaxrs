package com.opentable.jaxrs.clientfactory;

import org.skife.config.Config;
import org.skife.config.Default;

public interface JaxRsClientConfig
{
    @Config({"jaxrs.client.${clientName}.connect-timeout.millis", "jaxrs.client.default.connect-timeout.millis"})
    @Default("1000")
    long connectTimeoutMillis();

    @Config({"jaxrs.client.${clientName}.socket-timeout.millis", "jaxrs.client.default.socket-timeout.millis"})
    @Default("10000")
    long socketTimeoutMillis();

    @Config({"jaxrs.client.${clientName}.auth.basic.username", "jaxrs.client.default.auth.basic.username"})
    @Default("")
    String basicAuthUserName();

    @Config({"jaxrs.client.${clientName}.auth.basic.password", "jaxrs.client.default.auth.basic.password"})
    @Default("")
    String basicAuthPassword();

    @Config({"jaxrs.client.${clientName}.connection-pool.size", "jaxrs.client.default.connection-pool.size"})
    @Default("40")
    int connectionPoolSize();

    @Config({"jaxrs.client.${clientName}.http.max.total.connections", "jaxrs.client.default.http.max.total.connections"})
    @Default("100")
    int httpClientMaxTotalConnections();

    @Config({"jaxrs.client.${clientName}.http.max.per.route.connections", "jaxrs.client.default.http.max.per.route.connections"})
    @Default("20")
    int httpClientDefaultMaxPerRoute();

    @Config({"jaxrs.client.${clientName}.enable-etcd-hacks", "jaxrs.client.default.enable-etcd-hacks"})
    @Default("true")
    boolean isEtcdHacksEnabled();
}
