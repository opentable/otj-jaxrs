/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.jaxrs;

import java.time.Duration;

/**
 * JAX-RS Client tunables.
 */
public interface JaxRsClientConfig
{
    JaxRsClientConfig DEFAULT = new JaxRsClientConfig() {};

    /**
     * Timeout to check out a connection from the connection pool.
     */
    //@Config({"jaxrs.client.${clientName}.pool.timeout", "jaxrs.client.default.pool.timeout"})
    //@Default("5s")
    default Duration getConnectionPoolTimeout() {
        return Duration.ofSeconds(5);
    }

    /**
     * Monitor connection pool for failure to acquire leases.
     */
    //@Config({"jaxrs.client.${clientName}.pool.warn-time", "jaxrs.client.default.pool.warn-time"})
    //@Default("1s")
    default Duration getConnectionPoolWarnTime() {
        return Duration.ofSeconds(1);
    }

    /**
     * Connection pool size.
     */
    //@Config({"jaxrs.client.${clientName}.pool.size", "jaxrs.client.default.pool.size"})
    //@Default("40")
    default int getConnectionPoolSize() {
        return 40;
    }

    /**
     * Timeout to establish initial connection.
     */
    //@Config({"jaxrs.client.${clientName}.connect-timeout", "jaxrs.client.default.connect-timeout"})
    //@Default("10s")
    default Duration getConnectTimeout() {
        return Duration.ofSeconds(10);
    }

    /**
     * Socket timeout.
     */
    //@Config({"jaxrs.client.${clientName}.socket-timeout", "jaxrs.client.default.socket-timeout"})
    //@Default("30s")
    default Duration getSocketTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * HTTP connection pool idle timeout.
     */
    default Duration getIdleTimeout() {
        return Duration.ofSeconds(20);
    }

    /**
     * Basic auth username.
     */
    //@Config({"jaxrs.client.${clientName}.auth.basic.username", "jaxrs.client.default.auth.basic.username"})
    //@DefaultNull
    default String getBasicAuthUserName() {
        return null;
    }

    /**
     * Basic auth password.
     */
    //@Config({"jaxrs.client.${clientName}.auth.basic.password", "jaxrs.client.default.auth.basic.password"})
    //@DefaultNull
    default String getBasicAuthPassword() {
        return null;
    }

    /**
     * Maximum total connections allowed.
     */
    //@Config({"jaxrs.client.${clientName}.max-connections", "jaxrs.client.default.max-connections"})
    //@Default("100")
    default int getHttpClientMaxTotalConnections() {
        return 100;
    }

    /**
     * Maximum connections per-route.
     */
    //@Config({"jaxrs.client.${clientName}.max-route-connections", "jaxrs.client.default.max-route-connections"})
    //@Default("20")
    default int getHttpClientDefaultMaxPerRoute() {
        return 20;
    }

    /**
     * Special hacks for etcd, especially allowing PUTs to 307 redirect.
     */
    //@Config({"jaxrs.client.${clientName}.enable-etcd-hacks", "jaxrs.client.default.enable-etcd-hacks"})
    //@Default("true")
    default boolean isEtcdHacksEnabled() {
        return true;
    }
}
