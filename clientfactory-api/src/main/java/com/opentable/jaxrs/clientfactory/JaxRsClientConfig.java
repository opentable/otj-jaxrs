package com.opentable.jaxrs.clientfactory;

import org.skife.config.Config;
import org.skife.config.Default;

public interface JaxRsClientConfig
{
    @Config("jaxrs.client.connect-timeout.millis")
    @Default("1000")
    long connectTimeoutMillis();

    @Config("jaxrs.client.socket-timeout.millis")
    @Default("10000")
    long socketTimeoutMillis();

    @Config("jaxrs.client.auth.basic.username")
    @Default("")
    String basicAuthUserName();

    @Config("jaxrs.client.auth.basic.password")
    @Default("")
    String basicAuthPassword();

    @Config("jaxrs.client.connection-pool.size")
    @Default("40")
    int connectionPoolSize();

    @Config("jaxrs.client.http.max.total.connections")
    @Default("100")
    int httpClientMaxTotalConnections();

    @Config("jaxrs.client.http.max.per.route.connections")
    @Default("20")
    int httpClientDefaultMaxPerRoute();
}
