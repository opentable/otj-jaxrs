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

import org.immutables.value.Value;

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
@Value.Immutable
public interface JaxRsClientConfig
{
    JaxRsClientConfig DEFAULT = new JaxRsClientConfig() {};

    /**
     * Timeout to check out a connection from the connection pool.
     *
     * This connection pool checkout won't occur until you call {@link javax.ws.rs.client.InvocationInvocation#invoke()}.
     * If the time to get a connection surpasses this value, a runtime exception will be thrown.
     *
     * Supported: resteasy-apache, resteasy
     * Unsupported: resteasy, jersey
     */
    @Value.Default
    default Duration getConnectionPoolTimeout() {
        return Duration.ofSeconds(5);
    }


    @Value.Default
    default boolean isLimitConnectionPool() { return true; }
    @Value.Default
    default int getMaxUsages() { return 5; }
    /**
     * Monitor connection pool for failure to acquire leases.
     *
     * This connection pool checkout won't occur until you call {@link javax.ws.rs.client.InvocationInvocation#invoke()}.
     * If the time to get a connection surpasses this value, a warning will be logged.
     *
     * Supported: resteasy-apache
     * Unsupported: resteasy, jersey
     */
    @Value.Default
    default Duration getConnectionPoolWarnTime() {
        return Duration.ofSeconds(1);
    }

    /**
     * Connection pool size.
     *
     * Supported: resteasy-apache, jersey
     * Unsupported: resteasy
     */
    @Value.Default
    default int getConnectionPoolSize() {
        return 40;
    }


    /**
     * Disable Jetty HttpClient gzip compression and decompression.
     * Content decoding confuses clients so we default to true.
     * ot.webclient.(name).disableCompression
     * @return boolean
     */
    default boolean getDisableCompression() {
        return false;
    }

    /**
     * Maximum number of simultaneous asynchronous requests
     * in-flight awaiting resource (e.g. connection) availability
     * before we reject additional requests.
     *
     * Supported: resteasy-apache, resteasy
     * Unsupported: jersey
     */
    @Value.Default
    default int getAsyncQueueLimit() {
        return 1000;
    }

    /**
     * Timeout to establish initial connection.
     *
     * Supported: resteasy-apache, resteasy
     * Unsupported: jersey
     */
    @Value.Default
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
    @Value.Default
    @Deprecated
    default Duration getSocketTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * HTTP connection idle timeout.
     *
     * Supported: all
     */
    @Value.Default
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
    @Value.Default
    default int getHttpClientDefaultMaxPerRoute() {
        return getConnectionPoolSize();
    }

    /**
     * Whether cookies should be handled.
     *
     * Supported: resteasy, resteasy-apache
     * Unsupported: jersey
     */
    @Value.Default
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
    @Value.Default
    default int getExecutorThreads() { return -1; };

    /**
     * Get the HTTP proxy host to proxy this client's requests through
     * @return the proxy host, or an empty string if the client should not use a proxy
     *
     * Supported: resteasy
     * Unsupported: resteasy-apache, jersey
     */
    @Value.Default
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
    @Value.Default
    default int getProxyPort() {
        return 0;
    }


    /**
     * Disable TLS13 - Java 11
     */
    @Value.Default
    default boolean isDisableTLS13() {
        return false;
    }

    /**
     * Get a user agent for the client to use
     *
     * Supported: resteasy, resteasy-apache
     * Unsupported: jersey
     *
     * @return the user agent string to use
     */
    @Value.Default
    default String getUserAgent() {
        return "OT-HTTP-Client"; // Choosing a default that leaks minimal information for security reasons
    }

    /**
     * May replace the user agent with value of getUserAgent
     * depending on setting of isRemoveUserAgent.
     *      isReplaceUserAgent      isRemoveUserAgent Outcome
     *      true or false           true              User Agent is cleared
     *      true                    false             User Agent is set to value of getUserAgent
     *      false                   false             User Agent is set to Jetty's default user agent.
     */
    @Value.Default
    default boolean isReplaceUserAgent() {
        return true;
    }

    /**
     * Clear the User Agent. In these cases, the user agent specified above is ignored, and NO user agent is set.
     * The motivation for this is to avoid double User-Agent headers which can occur if your own code must programmatically
     * set the User-Agent field. (Jetty unfortunately fails to respect this)
     *      isReplaceUserAgent      isRemoveUserAgent Outcome
     *      true or false           true              User Agent is cleared
     *      true                    false             User Agent is set to value of getUserAgent
     *      false                   false             User Agent is set to Jetty's default user agent.
     * @return if the user agent is supposed to be cleared.*/
    @Value.Default
    default boolean isRemoveUserAgent() {
        return false;
    }

}
