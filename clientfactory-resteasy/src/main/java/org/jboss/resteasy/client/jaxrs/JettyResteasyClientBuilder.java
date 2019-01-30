package org.jboss.resteasy.client.jaxrs;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.engines.jetty.JettyClientEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.jaxrs.JaxRsClientConfig;
import com.opentable.jaxrs.TlsProvider;

public class JettyResteasyClientBuilder extends ResteasyClientBuilderImpl {
    private static final Logger LOG = LoggerFactory.getLogger(JettyResteasyClientBuilder.class);

    private final String clientName;
    private final JaxRsClientConfig config;
    private final TlsProvider provider;

    public JettyResteasyClientBuilder(String clientName, JaxRsClientConfig config, TlsProvider provider) {
        this.clientName = clientName;
        this.config = config;
        this.provider = provider;
    }

    @Override
    public ResteasyClient build() {
        final HttpClient client = createHttpClient();
        client.setIdleTimeout(config.getIdleTimeout().toMillis());
        client.setAddressResolutionTimeout(config.getConnectTimeout().toMillis());
        client.setConnectTimeout(config.getConnectTimeout().toMillis());
        client.setMaxConnectionsPerDestination(config.getHttpClientDefaultMaxPerRoute());
        client.setRemoveIdleDestinations(true);
        if(StringUtils.isNotBlank(config.getProxyHost()) && config.getProxyPort() != 0) {
            HttpProxy proxy = new HttpProxy(config.getProxyHost(), config.getProxyPort());
            client.getProxyConfiguration().getProxies().add(proxy);
        }
        if (config.isCookieHandlingEnabled()) {
            client.setCookieStore(new HttpCookieStore());
        }

        final ClientConfiguration cc = new ClientConfiguration(getProviderFactory());
        properties.forEach(cc::property);

        return new ResteasyClientImpl(new JettyClientEngine(client), asyncExecutor, true, scheduledExecutorService, cc);
    }

    private SslContextFactory createSslFactory() {
        final SslContextFactory factory = new SslContextFactory();
        factory.setTrustAll(disableTrustManager);
        Optional.ofNullable(clientKeyStore).ifPresent(ks-> {
            factory.setKeyStore(ks);
            factory.setKeyStorePassword(clientPrivateKeyPassword);
        });
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

    private HttpClient createHttpClient() {
        final HttpClient hc = new HttpClient(createSslFactory());
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
