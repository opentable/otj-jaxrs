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
 *
 * <p>
 * In your properties file, the config options are prefixed with &ldquo;{@code jaxrs.client.${clientName}.}&rdquo;.
 * Currently, the config parameter names themselves are the {@link java.beans.PropertyDescriptor}-style names.
 * So, for example, you might include the following in your config file:
 * {@code jaxrs.client.foo.connectionPoolSize=10}.
 *
 * Many settings are supported only for specific engines
 * resteasy - resteasy implementation backed by jetty. This is the default
 * resteasy-apache - old resteasy implementation using apache http client.
 * jersey - jersey implementation, also backed by apache http client.
 */
public interface JaxRsClientConfig
{
    JaxRsClientConfig DEFAULT = new JaxRsClientConfig() {};

    /**
     * Timeout to check out a connection from the connection pool.
     *
     * This connection pool checkout won't occur until you call {@link javax.ws.rs.client.InvocationInvocation#invoke()}.
     * If the time to get a connection surpasses this value, a runtime exception will be thrown.
     *
     * Supported: resteasy-apache
     * Unsupported: resteasy, jersey
     */
    default Duration getConnectionPoolTimeout() {
        return Duration.ofSeconds(5);
    }

    /**
     * Monitor connection pool for failure to acquire leases.
     *
     * This connection pool checkout won't occur until you call {@link javax.ws.rs.client.InvocationInvocation#invoke()}.
     * If the time to get a connection surpasses this value, a warning will be logged.
     *
     * Supported: resteasy-apache
     * Unsupported: resteasy, jersey
     */
    default Duration getConnectionPoolWarnTime() {
        return Duration.ofSeconds(1);
    }

    /**
     * Connection pool size.
     *
     * Supported: resteasy-apache, jersey
     * Unsupported: resteasy
     */
    default int getConnectionPoolSize() {
        return 40;
    }

    /**
     * Maximum number of simultaneous asynchronous requests
     * in-flight awaiting resource (e.g. connection) availability
     * before we reject additional requests.
     *
     * Supported: resteasy-apache, resteasy
     * Unsupported: jersey
     */
    default int getAsyncQueueLimit() {
        return 1000;
    }

    /**
     * Timeout to establish initial connection.
     *
     * Supported: resteasy-apache, resteasy
     * Unsupported: jersey
     */
    default Duration getConnectTimeout() {
        return Duration.ofSeconds(10);
    }

    /**
     * Socket timeout.  Not used for asynchronous capable engines.
     *
     * Supported: resteasy-apache, jersey
     * Unsupported: resteasy
     *
     * @see java.net.SocketOptions#SO_TIMEOUT
     */
    @Deprecated
    default Duration getSocketTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * HTTP connection idle timeout.
     *
     * Supported: all
     */
    default Duration getIdleTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * Maximum connections per-route.
     *
     * E.g., if you want to have lots of connections open to another microservice, and it's hosted on a number
     * of hosts ("routes") smaller than the number of connections you want to maintain.  By default, is
     * {@link #getConnectionPoolSize()}, since you can't have more connections than this.
     *
     * Supported: all
     */
    default int getHttpClientDefaultMaxPerRoute() {
        return getConnectionPoolSize();
    }

    /**
     * Whether cookies should be handled.
     *
     * Supported: resteasy, resteasy-apache
     * Unsupported: jersey
     */
    default boolean isCookieHandlingEnabled() {
        return false;
    }

    /**
     * Number of Executor Threads. Defaults to -1.
     * If set to -1, will try to autosize for cores. Not implemented for Jersey.
     *
     * Supported: resteasy, resteasy-apache
     * Unsupported: jersey
     */
    default int getExecutorThreads() { return -1; };

    /**
     * Get the HTTP proxy host to proxy this client's requests through
     * @return the proxy host, or an empty string if the client should not use a proxy
     *
     * Supported: resteasy
     * Unsupported: resteasy-apache, jersey
     */
    default String getProxyHost(){
        return "";
    }

    /**
     * Get the HTTP port of the proxy server to proxy this client's requests through
     * @return the HTTP port, or 0 if the client should not use a proxy
     *
     * Supported: resteasy
     * Unsupported: resteasy-apache, jersey
     */
    default int getProxyPort() {
        return 0;
    }


    /**
     * Disable TLS13 - Java 11
     */
    default boolean isDisableTLS13() {
        return false;
    }

}
