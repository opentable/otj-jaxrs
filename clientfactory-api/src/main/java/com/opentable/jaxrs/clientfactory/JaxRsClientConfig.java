package com.opentable.jaxrs.clientfactory;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;
import org.skife.config.TimeSpan;

public interface JaxRsClientConfig
{
    @Config({"jaxrs.client.${clientName}.pool-timeout", "jaxrs.client.default.pool-timeout"})
    @Default("1s")
    TimeSpan connectionPoolTimeout();

    @Config({"jaxrs.client.${clientName}.connect-timeout", "jaxrs.client.default.connect-timeout"})
    @Default("1s")
    TimeSpan connectTimeout();

    @Config({"jaxrs.client.${clientName}.socket-timeout", "jaxrs.client.default.socket-timeout"})
    @Default("10s")
    TimeSpan socketTimeout();

    @Config({"jaxrs.client.${clientName}.auth.basic.username", "jaxrs.client.default.auth.basic.username"})
    @DefaultNull
    String basicAuthUserName();

    @Config({"jaxrs.client.${clientName}.auth.basic.password", "jaxrs.client.default.auth.basic.password"})
    @DefaultNull
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
