package org.jboss.resteasy.client.jaxrs;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.engines.jetty.JettyClientEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;

import com.opentable.jaxrs.JaxRsClientConfig;

public class JettyResteasyClientBuilder extends ResteasyClientBuilder {

    private final String clientName;
    private final JaxRsClientConfig config;

    public JettyResteasyClientBuilder(String clientName, JaxRsClientConfig config) {
        this.clientName = clientName;
        this.config = config;
    }

    @Override
    public ResteasyClient build() {
        final HttpClient client = createHttpClient();
        client.setIdleTimeout(config.getIdleTimeout().toMillis());
        client.setAddressResolutionTimeout(config.getConnectTimeout().toMillis());
        client.setConnectTimeout(config.getConnectTimeout().toMillis());
        client.setMaxConnectionsPerDestination(config.getHttpClientDefaultMaxPerRoute());
        client.setRemoveIdleDestinations(true);
        if (config.isCookieHandlingEnabled()) {
            client.setCookieStore(new HttpCookieStore());
        }

        final ClientConfiguration cc = new ClientConfiguration(getProviderFactory());
        properties.forEach(cc::property);

        return new ResteasyClient(new JettyClientEngine(client), asyncExecutor, true, scheduledExecutorService, cc);
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
