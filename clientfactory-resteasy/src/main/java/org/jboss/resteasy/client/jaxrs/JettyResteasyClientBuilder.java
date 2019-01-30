package org.jboss.resteasy.client.jaxrs;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.engines.jetty.JettyClientEngine;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientImpl;
import org.jboss.resteasy.core.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.jaxrs.JaxRsClientConfig;
import com.opentable.jaxrs.TlsProvider;

public class JettyResteasyClientBuilder extends ResteasyClientBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(JettyResteasyClientBuilder.class);

    private final String clientName;
    private final JaxRsClientConfig config;
    private final TlsProvider provider;
    protected KeyStore truststore;
    protected KeyStore clientKeyStore;
    protected String clientPrivateKeyPassword;
    protected boolean disableTrustManager;
    protected HostnameVerificationPolicy policy = HostnameVerificationPolicy.WILDCARD;
    protected ResteasyProviderFactory providerFactory;
    protected ExecutorService asyncExecutor;
    protected ScheduledExecutorService scheduledExecutorService;
    protected boolean cleanupExecutor;
    protected SSLContext sslContext;
    protected Map<String, Object> properties = new HashMap<String, Object>();
    protected ClientHttpEngine httpEngine;
    protected int connectionPoolSize = 50;
    protected int maxPooledPerRoute = 0;
    protected long connectionTTL = -1;
    protected TimeUnit connectionTTLUnit = TimeUnit.MILLISECONDS;
    protected long socketTimeout = -1;
    protected TimeUnit socketTimeoutUnits = TimeUnit.MILLISECONDS;
    protected long establishConnectionTimeout = -1;
    protected TimeUnit establishConnectionTimeoutUnits = TimeUnit.MILLISECONDS;
    protected int connectionCheckoutTimeoutMs = -1;
    protected HostnameVerifier verifier = null;
    protected int responseBufferSize;
    protected List<String> sniHostNames = new ArrayList<>();


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

        return new JettyRestEasyClient(new JettyClientEngine(client), asyncExecutor, true, scheduledExecutorService, cc);
    }

    public static class JettyRestEasyClient extends ResteasyClientImpl {

        protected JettyRestEasyClient(final ClientHttpEngine httpEngine, final ExecutorService asyncInvocationExecutor, final boolean cleanupExecutor, final ScheduledExecutorService scheduledExecutorService, final ClientConfiguration configuration) {
            super(httpEngine, asyncInvocationExecutor, cleanupExecutor, scheduledExecutorService, configuration);
        }
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

    /*
        End customization.... the only other non vanilla changes avoid bringing in Apache Http CLient
        for the proxy.
     */

    /**
     * Changing the providerFactory will wipe clean any registered components or properties.
     *
     * @param providerFactory provider factory
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder providerFactory(ResteasyProviderFactory providerFactory)
    {
        this.providerFactory = providerFactory;
        return this;
    }

    /**
     * Executor to use to run AsyncInvoker invocations.
     *
     * @param asyncExecutor executor service
     * @return an updated client builder instance
     * @deprecated use {@link JettyResteasyClientBuilder#executorService(ExecutorService)} instead
     */
    @Deprecated
    public JettyResteasyClientBuilder asyncExecutor(ExecutorService asyncExecutor)
    {
        return asyncExecutor(asyncExecutor, false);
    }

    /**
     * Executor to use to run AsyncInvoker invocations.
     *
     * @param asyncExecutor executor service
     * @param cleanupExecutor true if the Client should close the executor when it is closed
     * @return an updated client builder instance
     */
    @Deprecated
    public JettyResteasyClientBuilder asyncExecutor(ExecutorService asyncExecutor, boolean cleanupExecutor)
    {
        this.asyncExecutor = asyncExecutor;
        this.cleanupExecutor = cleanupExecutor;
        return this;
    }

    /**
     * If there is a connection pool, set the time to live in the pool.
     *
     * @param ttl time to live
     * @param unit the time unit of the ttl argument
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder connectionTTL(long ttl, TimeUnit unit)
    {
        this.connectionTTL = ttl;
        this.connectionTTLUnit = unit;
        return this;
    }

    @Override
    public JettyResteasyClientBuilder readTimeout(long timeout, TimeUnit unit)
    {
        this.socketTimeout = timeout;
        this.socketTimeoutUnits = unit;
        return this;
    }

    @Override
    public JettyResteasyClientBuilder connectTimeout(long timeout, TimeUnit unit)
    {
        this.establishConnectionTimeout = timeout;
        this.establishConnectionTimeoutUnits = unit;
        return this;
    }

    /**
     * If connection pooling enabled, how many connections to pool per url?
     *
     * @param maxPooledPerRoute max pool size per url
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder maxPooledPerRoute(int maxPooledPerRoute)
    {
        this.maxPooledPerRoute = maxPooledPerRoute;
        return this;
    }

    /**
     * If connection pooling is enabled, how long will we wait to get a connection?
     * @param timeout the timeout
     * @param unit the units the timeout is in
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder connectionCheckoutTimeout(long timeout, TimeUnit unit)
    {
        this.connectionCheckoutTimeoutMs = (int) TimeUnit.MILLISECONDS.convert(timeout, unit);
        return this;
    }

    /**
     * Number of connections allowed to pool.
     *
     * @param connectionPoolSize connection pool size
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder connectionPoolSize(int connectionPoolSize)
    {
        this.connectionPoolSize = connectionPoolSize;
        return this;
    }

    /**
     * Response stream is wrapped in a BufferedInputStream.  Default is 8192.  Value of 0 will not wrap it.
     * Value of -1 will use a SelfExpandingBufferedInputStream.
     *
     * @param size response buffer size
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder responseBufferSize(int size)
    {
        this.responseBufferSize = size;
        return this;
    }


    /**
     * Disable trust management and hostname verification.  <i>NOTE</i> this is a security
     * hole, so only set this option if you cannot or do not want to verify the identity of the
     * host you are communicating with.
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder disableTrustManager()
    {
        this.disableTrustManager = true;
        return this;
    }

    /**
     * SSL policy used to verify hostnames
     *
     * @param policy SSL policy
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder hostnameVerification(HostnameVerificationPolicy policy)
    {
        this.policy = policy;
        return this;
    }

    /**
     * Negates all ssl and connection specific configuration
     *
     * @param httpEngine http engine
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder httpEngine(ClientHttpEngine httpEngine)
    {
        this.httpEngine = httpEngine;
        return this;
    }

    @Override
    public JettyResteasyClientBuilder sslContext(SSLContext sslContext)
    {
        this.sslContext = sslContext;
        return this;
    }

    @Override
    public JettyResteasyClientBuilder trustStore(KeyStore truststore)
    {
        this.truststore = truststore;
        return this;
    }

    @Override
    public JettyResteasyClientBuilder keyStore(KeyStore keyStore, String password)
    {
        this.clientKeyStore = keyStore;
        this.clientPrivateKeyPassword = password;
        return this;
    }

    @Override
    public JettyResteasyClientBuilder keyStore(KeyStore keyStore, char[] password)
    {
        this.clientKeyStore = keyStore;
        this.clientPrivateKeyPassword = new String(password);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder property(String name, Object value)
    {
        getProviderFactory().property(name, value);
        return this;
    }

    /**
     * Adds a TLS/SSL SNI Host Name for authentication.
     *
     * @param sniHostNames host names
     * @return an updated client builder instance
     */
    public JettyResteasyClientBuilder sniHostNames(String... sniHostNames) {
        this.sniHostNames.addAll(Arrays.asList(sniHostNames));
        return this;
    }

    /**
     * Specify a default proxy.  Default port and schema will be used.
     *
     * @param hostname host name
     * @return an updated client builder instance
     */
    public ResteasyClientBuilder defaultProxy(String hostname)
    {
        return defaultProxy(hostname, -1, null);
    }

    /**
     * Specify a default proxy host and port.  Default schema will be used.
     *
     * @param hostname host name
     * @param port port
     * @return an updated client builder instance
     */
    public ResteasyClientBuilder defaultProxy(String hostname, int port)
    {
        return defaultProxy(hostname, port, null);
    }

    @Override
    public ResteasyClientBuilder defaultProxy(final String hostname, final int port, final String scheme) {
        return null;
    }


    public ResteasyProviderFactory getProviderFactory()
    {
        if (providerFactory == null)
        {
            // create a new one
            providerFactory = new LocalResteasyProviderFactory(ResteasyProviderFactory.newInstance());
            RegisterBuiltin.register(providerFactory);
        }
        return providerFactory;
    }

    protected ResteasyClient createResteasyClient(ClientHttpEngine engine,ExecutorService executor, boolean cleanupExecutor, ScheduledExecutorService scheduledExecutorService, ClientConfiguration config ) {
        return new JettyRestEasyClient(engine, executor, cleanupExecutor, scheduledExecutorService, config);
    }

    @Override
    public JettyResteasyClientBuilder hostnameVerifier(HostnameVerifier verifier)
    {
        this.verifier = verifier;
        return this;
    }

    @Override
    public Configuration getConfiguration()
    {
        return getProviderFactory().getConfiguration();
    }

    @Override
    public JettyResteasyClientBuilder register(Class<?> componentClass)
    {
        getProviderFactory().register(componentClass);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder register(Class<?> componentClass, int priority)
    {
        getProviderFactory().register(componentClass, priority);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder register(Class<?> componentClass, Class<?>... contracts)
    {
        getProviderFactory().register(componentClass, contracts);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts)
    {
        getProviderFactory().register(componentClass, contracts);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder register(Object component)
    {
        getProviderFactory().register(component);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder register(Object component, int priority)
    {
        getProviderFactory().register(component, priority);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder register(Object component, Class<?>... contracts)
    {
        getProviderFactory().register(component, contracts);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder register(Object component, Map<Class<?>, Integer> contracts)
    {
        getProviderFactory().register(component, contracts);
        return this;
    }

    @Override
    public JettyResteasyClientBuilder withConfig(Configuration config)
    {
        providerFactory = new LocalResteasyProviderFactory(new ResteasyProviderFactoryImpl());
        providerFactory.setProperties(config.getProperties());
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
    public ResteasyClientBuilder executorService(ExecutorService executorService)
    {
        return asyncExecutor(executorService, false);
    }

    @Override
    public ResteasyClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService)
    {
        this.scheduledExecutorService = scheduledExecutorService;
        return this;
    }

    @Override
    public long getConnectionTTL(TimeUnit unit)
    {
        return connectionTTLUnit.equals(unit) ? connectionTTL : unit.convert(connectionTTL, connectionTTLUnit);
    }

    @Override
    public int getMaxPooledPerRoute()
    {
        return maxPooledPerRoute;
    }

    @Override
    public long getConnectionCheckoutTimeout(TimeUnit unit)
    {
        return TimeUnit.MILLISECONDS.equals(unit) ? connectionCheckoutTimeoutMs : unit.convert(connectionCheckoutTimeoutMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public int getConnectionPoolSize()
    {
        return connectionPoolSize;
    }

    @Override
    public int getResponseBufferSize()
    {
        return responseBufferSize;
    }

    @Override
    public boolean isTrustManagerDisabled()
    {
        return disableTrustManager;
    }

    @Override
    public HostnameVerificationPolicy getHostnameVerification()
    {
        return policy;
    }

    @Override
    public ClientHttpEngine getHttpEngine()
    {
        return httpEngine;
    }

    @Override
    public ResteasyClientBuilder useAsyncHttpEngine() {
        return this;
    }

    @Override
    public boolean isUseAsyncHttpEngine()
    {
        return httpEngine != null;
    }

    @Override
    public List<String> getSniHostNames()
    {
        return sniHostNames;
    }

    @Override
    public String getDefaultProxyHostname()
    {
        return null;
    }

    @Override
    public int getDefaultProxyPort()
    {
        return  -1;
    }

    @Override
    public String getDefaultProxyScheme()
    {
        return null;
    }
    @Override
    public long getReadTimeout(TimeUnit unit)
    {
        return socketTimeoutUnits.equals(unit) ? socketTimeout : unit.convert(socketTimeout, socketTimeoutUnits);
    }

    @Override
    public long getConnectionTimeout(TimeUnit unit)
    {
        return establishConnectionTimeoutUnits.equals(unit) ? establishConnectionTimeout : unit.convert(establishConnectionTimeout, establishConnectionTimeoutUnits);
    }

    @Override
    public SSLContext getSSLContext()
    {
        return sslContext;
    }

    @Override
    public KeyStore getKeyStore()
    {
        return clientKeyStore;
    }

    @Override
    public String getKeyStorePassword()
    {
        return clientPrivateKeyPassword;
    }

    @Override
    public KeyStore getTrustStore()
    {
        return truststore;
    }

    @Override
    public HostnameVerifier getHostnameVerifier()
    {
        return verifier;
    }
}
