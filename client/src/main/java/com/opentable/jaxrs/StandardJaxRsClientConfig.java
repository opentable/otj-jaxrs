package com.opentable.jaxrs;

import java.time.Duration;
import java.util.Optional;

/**
 * An implementation of JaxRsClientConfig that is mutable. Values use defaults unless set.
 */
public class StandardJaxRsClientConfig implements JaxRsClientConfig {

    private Optional<Duration> connectionPoolTimeout = Optional.empty();
    private Optional<Duration> connectionPoolWarnTime = Optional.empty();
    private Optional<Integer> connectionPoolSize = Optional.empty();
    private Optional<Integer> asyncQueueLimit = Optional.empty();
    private Optional<Duration> connectTimeout = Optional.empty();
    private Optional<Duration> idleTimeout = Optional.empty();
    private Optional<Integer> httpClientDefaultMaxPerRoute = Optional.empty();
    private Optional<Boolean> cookieHandlingEnabled = Optional.empty();
    private Optional<Integer> executorThreads = Optional.empty();
    private Optional<String> proxyHost = Optional.empty();
    private Optional<Integer> proxyPort = Optional.empty();
    private Optional<Boolean> disableTLS13 = Optional.empty();
    private Optional<String> userAgent = Optional.empty();

    public void setConnectionPoolTimeout(Optional<Duration> connectionPoolTimeout) {
        this.connectionPoolTimeout = connectionPoolTimeout;
    }

    public void setConnectionPoolWarnTime(Optional<Duration> connectionPoolWarnTime) {
        this.connectionPoolWarnTime = connectionPoolWarnTime;
    }

    public void setConnectionPoolSize(Optional<Integer> connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public void setAsyncQueueLimit(Optional<Integer> asyncQueueLimit) {
        this.asyncQueueLimit = asyncQueueLimit;
    }

    public void setConnectTimeout(Optional<Duration> connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setIdleTimeout(Optional<Duration> idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public void setHttpClientDefaultMaxPerRoute(Optional<Integer> httpClientDefaultMaxPerRoute) {
        this.httpClientDefaultMaxPerRoute = httpClientDefaultMaxPerRoute;
    }

    public void setCookieHandlingEnabled(Optional<Boolean> cookieHandlingEnabled) {
        this.cookieHandlingEnabled = cookieHandlingEnabled;
    }

    public void setExecutorThreads(Optional<Integer> executorThreads) {
        this.executorThreads = executorThreads;
    }

    public void setProxyHost(Optional<String> proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(Optional<Integer> proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setDisableTLS13(Optional<Boolean> disableTLS13) {
        this.disableTLS13 = disableTLS13;
    }

    public void setUserAgent(Optional<String> userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public Duration getConnectionPoolTimeout() {
        if (connectionPoolTimeout.isPresent()) {
            return connectionPoolTimeout.get();
        }
        return JaxRsClientConfig.super.getConnectionPoolTimeout();
    }

    @Override
    public Duration getConnectionPoolWarnTime() {
        if (connectionPoolWarnTime.isPresent()) {
            return connectionPoolWarnTime.get();
        }
        return JaxRsClientConfig.super.getConnectionPoolWarnTime();
    }

    @Override
    public int getConnectionPoolSize() {
        if (connectionPoolSize.isPresent()) {
            return connectionPoolSize.get();
        }
        return JaxRsClientConfig.super.getConnectionPoolSize();
    }

    @Override
    public int getAsyncQueueLimit() {
        if (asyncQueueLimit.isPresent()) {
            return asyncQueueLimit.get();
        }
        return JaxRsClientConfig.super.getAsyncQueueLimit();
    }

    @Override
    public Duration getConnectTimeout() {
        if (connectTimeout.isPresent()) {
            return connectTimeout.get();
        }
        return JaxRsClientConfig.super.getConnectTimeout();
    }

    @Override
    public Duration getIdleTimeout() {
        if (idleTimeout.isPresent()) {
            return idleTimeout.get();
        }
        return JaxRsClientConfig.super.getIdleTimeout();
    }

    @Override
    public int getHttpClientDefaultMaxPerRoute() {
        if (httpClientDefaultMaxPerRoute.isPresent()) {
            return httpClientDefaultMaxPerRoute.get();
        }
        return JaxRsClientConfig.super.getHttpClientDefaultMaxPerRoute();
    }

    @Override
    public boolean isCookieHandlingEnabled() {
        if (cookieHandlingEnabled.isPresent()) {
            return cookieHandlingEnabled.get();
        }
        return JaxRsClientConfig.super.isCookieHandlingEnabled();
    }

    @Override
    public int getExecutorThreads() {
        if (executorThreads.isPresent()) {
            return executorThreads.get();
        }
        return JaxRsClientConfig.super.getExecutorThreads();
    }

    @Override
    public String getProxyHost() {
        if (proxyHost.isPresent()) {
            return proxyHost.get();
        }
        return JaxRsClientConfig.super.getProxyHost();
    }

    @Override
    public int getProxyPort() {
        if (proxyPort.isPresent()) {
            return proxyPort.get();
        }
        return JaxRsClientConfig.super.getProxyPort();
    }

    @Override
    public boolean isDisableTLS13() {
        if (disableTLS13.isPresent()) {
            return disableTLS13.get();
        }
        return JaxRsClientConfig.super.isDisableTLS13();
    }

    @Override
    public String getUserAgent() {
        if (userAgent.isPresent()) {
            return userAgent.get();
        }
        return JaxRsClientConfig.super.getUserAgent();
    }

}
