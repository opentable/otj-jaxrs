package org.jboss.resteasy.client.jaxrs;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.engines.JettyClientEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;

import com.opentable.jaxrs.JaxRsClientConfig;

public class JettyResteasyClientBuilder extends ResteasyClientBuilder {

    private final String clientName;
    private final JaxRsClientConfig config;
    private final BiConsumer<ResteasyClientBuilder, HttpClient> customizer;

    public JettyResteasyClientBuilder(String clientName, JaxRsClientConfig config, BiConsumer<ResteasyClientBuilder, HttpClient> customizer) {
        this.clientName = clientName;
        this.config = config;
        this.customizer = customizer;
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
        customizer.accept(this, client);

        final ClientConfiguration cc = new ClientConfiguration(getProviderFactory());
        for (Map.Entry<String, Object> entry : properties.entrySet())
        {
           cc.property(entry.getKey(), entry.getValue());
        }

        return new ResteasyClient(new JettyClientEngine(client), asyncExecutor, cleanupExecutor, scheduledExecutorService, cc);
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
