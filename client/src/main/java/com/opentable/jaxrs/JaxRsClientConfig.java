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

import javax.ws.rs.client.Invocation;

/**
 * JAX-RS Client tunables.
 *
 * In your properties file, the config options are prefixed with &ldquo;{@code jaxrs.client.${clientName}}&rdquo;.
 * Currently, the config values themselves are the same as the actual method names(!).
 * So, for example, you might include the following in your config file:
 * {@code jaxrs.client.foo.getConnectionPoolSize=10}
 */
public interface JaxRsClientConfig
{
    JaxRsClientConfig DEFAULT = new JaxRsClientConfig() {};

    /**
     * Timeout to check out a connection from the connection pool.
     *
     * This connection pool checkout won't occur until you call {@link Invocation#invoke()}.  If the time to get a
     * connection surpasses this value, a runtime exception will be thrown.
     */
    default Duration getConnectionPoolTimeout() {
        return Duration.ofSeconds(5);
    }

    /**
     * Monitor connection pool for failure to acquire leases.
     *
     * This connection pool checkout won't occur until you call {@link Invocation#invoke()}.  If the time to get a
     * connection surpasses this value, a warning will be logged.
     */
    default Duration getConnectionPoolWarnTime() {
        return Duration.ofSeconds(1);
    }

    /**
     * Connection pool size.
     */
    default int getConnectionPoolSize() {
        return 40;
    }

    /**
     * Timeout to establish initial connection.
     */
    default Duration getConnectTimeout() {
        return Duration.ofSeconds(10);
    }

    /**
     * Socket timeout.
     *
     * @see java.net.SocketOptions#SO_TIMEOUT
     */
    default Duration getSocketTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * HTTP connection pool idle connection eviction threshold.
     *
     * @see org.apache.http.impl.client.HttpClientBuilder#evictIdleConnections
     */
    default Duration getIdleTimeout() {
        return Duration.ofSeconds(20);
    }

    /**
     * Basic auth username.
     */
    default String getBasicAuthUserName() {
        return null;
    }

    /**
     * Basic auth password.
     */
    default String getBasicAuthPassword() {
        return null;
    }

    /**
     * Maximum connections per-route.
     *
     * E.g., if you want to have lots of connections open to another microservice, and it's hosted on a number
     * of hosts ("routes") smaller than the number of connections you want to maintain.  By default, is
     * {@link #getConnectionPoolSize()}, since you can't have more connections than this.
     */
    default int getHttpClientDefaultMaxPerRoute() {
        return getConnectionPoolSize();
    }

    /**
     * Special hacks for etcd, especially allowing PUTs to 307 redirect.
     */
    default boolean isEtcdHacksEnabled() {
        return true;
    }
}
