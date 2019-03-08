package org.jboss.resteasy.client.jaxrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.engines.jetty.JettyClientEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.jaxrs.JaxRsClientConfig;
import com.opentable.jaxrs.TlsProvider;

public class JettyResteasyClientBuilder extends ResteasyClientBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(JettyResteasyClientBuilder.class);

    private final String clientName;
    private final JaxRsClientConfig config;
    private final TlsProvider provider;
    private final List<Consumer<SslContextFactory>> factoryCustomizers;

    public JettyResteasyClientBuilder(String clientName, JaxRsClientConfig config, TlsProvider provider, List<Consumer<SslContextFactory>> factoryCustomizers) {
        this.clientName = clientName;
        this.config = config;
        this.provider = provider;
        this.factoryCustomizers = factoryCustomizers == null ? new ArrayList<>() : factoryCustomizers;
    }

    @Override
    public ResteasyClient build() {
        final HttpClient client = createHttpClient(factoryCustomizers);
        client.setIdleTimeout(config.getIdleTimeout().toMillis());
        client.setAddressResolutionTimeout(config.getConnectTimeout().toMillis());
        client.setConnectTimeout(config.getConnectTimeout().toMillis());
        client.setMaxConnectionsPerDestination(config.getHttpClientDefaultMaxPerRoute());
        client.setRemoveIdleDestinations(true);
        LOG.info("Setting User-Agent for the {} HTTP client to {}", clientName, config.getUserAgent());
        client.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, config.getUserAgent()));
        if(StringUtils.isNotBlank(config.getProxyHost()) && config.getProxyPort() != 0) {
            HttpProxy proxy = new HttpProxy(config.getProxyHost(), config.getProxyPort());
            client.getProxyConfiguration().getProxies().add(proxy);
        }
        if (config.isCookieHandlingEnabled()) {
            client.setCookieStore(new HttpCookieStore());
        }

        final ClientConfiguration cc = new ClientConfiguration(getProviderFactory());
        properties.forEach(cc::property);

        return new ResteasyClient(new JettyClientEngine(client), asyncExecutor, true, scheduledExecutorService, cc);
    }

    private SslContextFactory createSslFactory(List<Consumer<SslContextFactory>> factoryCustomizers) {
        final SslContextFactory factory = new SslContextFactory();
        factory.setTrustAll(disableTrustManager);
        Optional.ofNullable(clientKeyStore).ifPresent(ks-> {
            factory.setKeyStore(ks);
            factory.setKeyStorePassword(clientPrivateKeyPassword);
        });
        factoryCustomizers.forEach(c -> c.accept(factory));
        Optional.ofNullable(truststore).ifPresent(factory::setTrustStore);
        Optional.ofNullable(sslContext).ifPresent(factory::setSslContext);
        if (provider != null) {
            provider.init((ts, ks) -> {
                try {
                    factory.reload(f -> {
                        f.setValidateCerts(true);
                        f.setValidatePeerCerts(true);
                        f.setKeyStorePassword("");
                        f.setKeyStore(ks);
                        f.setTrustStorePassword("");
                        f.setTrustStore(ts);
                    });
                    LOG.debug("Rotated client {} TLS keys to {}", factory, ks);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return factory;
    }

    private HttpClient createHttpClient(List<Consumer<SslContextFactory>> customizers) {
        final HttpClient hc = new HttpClient(createSslFactory(customizers));
        Optional.ofNullable(asyncExecutor).ifPresent(hc::setExecutor);
        hc.setConnectTimeout(TimeUnit.MILLISECONDS.convert(establishConnectionTimeout, establishConnectionTimeoutUnits));
        if (responseBufferSize > 0) {
            hc.setResponseBufferSize(responseBufferSize);
            hc.setRequestBufferSize(responseBufferSize);
        }
        return hc;
    }

    @Override
    public String toString() {
        return "JettyResteasyClientBuilder[" + clientName + "]";
    }
}
