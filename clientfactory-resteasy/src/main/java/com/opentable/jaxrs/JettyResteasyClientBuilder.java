package com.opentable.jaxrs;

import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.opentable.http.common.HttpClientBuilder;
import com.opentable.http.common.HttpClientCommonConfiguration;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.jetty.JettyClientEngine;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class JettyResteasyClientBuilder extends ClientBuilder {

    // Supported both via builder
    // and TLSProvider injection concept of Steven's
    // used in code mostly only via TLSProvider
    protected KeyStore truststore;
    protected KeyStore clientKeyStore;
    protected String clientPrivateKeyPassword;

    // Required by builder, not used in JaxRSClientFactoryImpl (so default implementation is used)
    protected SSLContext sslContext;
    protected ResteasyProviderFactory providerFactory;
    // This hasn't ever been set properly. Not setting it will cause SSEEvents to fail
    // but setting it, will cause another branch of code to go ... interesting
    protected ScheduledExecutorService scheduledExecutorService;

    // builder required, and set in JaxRSClientFactoryImpl
    protected Duration connectTimeout = Duration.ofMillis(15000L); // match jetty defaults
    protected HostnameVerifier hostnameVerifier;
    protected ExecutorService asyncExecutor;

    // extra customization beyond base ClientBuilder interface
    private final List<Consumer<SslContextFactory>> sslContextFactoryCustomizers;
    private final  boolean cleanupExecutor;
    private final HttpClientCommonConfiguration httpClientCommonConfiguration;
    private final HttpClientBuilder httpClientBuilder;

    public JettyResteasyClientBuilder(boolean cleanupExecutor, HttpClientCommonConfiguration httpClientCommonConfiguration, List<Consumer<SslContextFactory>> sslContextFactoryCustomizers) {
        this.httpClientCommonConfiguration = httpClientCommonConfiguration;
        this.sslContextFactoryCustomizers = sslContextFactoryCustomizers;
        this.cleanupExecutor = cleanupExecutor;
        this.httpClientBuilder = new HttpClientBuilder();
    }

    @Override
    public Client build() {
        final HttpClient client = createHttpClient(
                httpClientCommonConfiguration,
                sslContextFactoryCustomizers == null ? new ArrayList<>() : sslContextFactoryCustomizers
        );
        final ClientConfiguration cc = new ClientConfiguration(getProviderFactory());
        final ClientHttpEngine clientHttpEngine = new JettyClientEngine(client);
        return new JettyRestEasyClient(clientHttpEngine, asyncExecutor, cleanupExecutor, scheduledExecutorService, cc);
    }

    private SslContextFactory createSslFactory(SslContextFactory factory, List<Consumer<SslContextFactory>> factoryCustomizers) {
        factory.setHostnameVerifier(hostnameVerifier);
        Optional.ofNullable(clientKeyStore).ifPresent(ks-> {
            factory.setKeyStore(ks);
            factory.setKeyStorePassword(clientPrivateKeyPassword);
        });
        Optional.ofNullable(truststore).ifPresent(factory::setTrustStore);
        Optional.ofNullable(sslContext).ifPresent(factory::setSslContext);
        factoryCustomizers.forEach(c -> c.accept(factory));

        return factory;
    }

    private HttpClient createHttpClient(HttpClientCommonConfiguration httpClientCommonConfiguration, List<Consumer<SslContextFactory>> sslContextFactoryCustomizers) {
        final HttpClient hc = httpClientBuilder.build(httpClientCommonConfiguration, httpClientCommonConfiguration.getThreadPoolName());
        // These may be dynamically reconfigured in RestEasyBuilder, so we must always reapply.
        createSslFactory(hc.getSslContextFactory(), sslContextFactoryCustomizers);
        hc.setExecutor(asyncExecutor);
        hc.setConnectTimeout(connectTimeout.toMillis());
        hc.setAddressResolutionTimeout(connectTimeout.toMillis());
        return hc;
    }

    private ResteasyProviderFactory getProviderFactory() {
        if (providerFactory == null)
        {
            // create a new one
            providerFactory = new LocalResteasyProviderFactory(ResteasyProviderFactory.newInstance());
            RegisterBuiltin.register(providerFactory);
        }
        return providerFactory;
    }

    @Override
    public ClientBuilder readTimeout(long timeout, TimeUnit unit) {
        // Jetty doesn't support a readTimeout, only an idleTimeout
        return this;
    }

    @Override
    public ClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        this.connectTimeout = Duration.ofMillis(TimeUnit.MILLISECONDS.convert(timeout, unit));
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore truststore) {
        this.truststore = truststore;
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(final HostnameVerifier verifier) {
        this.hostnameVerifier = verifier;
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, String password) {
        this.clientKeyStore = keyStore;
        this.clientPrivateKeyPassword = password;
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        this.clientKeyStore = keyStore;
        this.clientPrivateKeyPassword = new String(password);
        return this;
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        getProviderFactory().property(name, value);
        return this;
    }

    @Override
    public Configuration getConfiguration() {
        return getProviderFactory().getConfiguration();
    }

    @Override
    public ClientBuilder register(Class<?> componentClass) {
        getProviderFactory().register(componentClass);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, int priority) {
        getProviderFactory().register(componentClass, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        getProviderFactory().register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        getProviderFactory().register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component) {
        getProviderFactory().register(component);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, int priority) {
        getProviderFactory().register(component, priority);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Class<?>... contracts) {
        getProviderFactory().register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        getProviderFactory().register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilder withConfig(Configuration config) {
        getProviderFactory().setProperties(config.getProperties());
        for (Class clazz : config.getClasses())
        {
            Map<Class<?>, Integer> contracts = config.getContracts(clazz);
            try {
                register(clazz, contracts);
            }
            catch (RuntimeException e) {
                throw new RuntimeException(Messages.MESSAGES.failedOnRegisteringClass(clazz.getName()), e);
            }
        }
        for (Object obj : config.getInstances())
        {
            Map<Class<?>, Integer> contracts = config.getContracts(obj.getClass());
            register(obj, contracts);
        }
        return this;
    }

    @Override
    public ClientBuilder executorService(ExecutorService executorService) {
        this.asyncExecutor = executorService;
        return this;
    }

    @Override
    public ClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        return this;
    }

    public static class JettyRestEasyClient extends ResteasyClientImpl {
        protected JettyRestEasyClient(final ClientHttpEngine httpEngine, final ExecutorService asyncInvocationExecutor, final boolean cleanupExecutor, final ScheduledExecutorService scheduledExecutorService, final ClientConfiguration configuration) {
            super(httpEngine, asyncInvocationExecutor, cleanupExecutor, scheduledExecutorService, configuration);
        }
    }
}
