package org.jboss.resteasy.client.jaxrs;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.client.jaxrs.engines.jetty.JettyClientEngine;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyResteasyClientBuilder extends ResteasyClientBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(JettyResteasyClientBuilder.class);

    // These are items added to the standard builder
    private String clientName = "unknown";
    private List<Consumer<SslContextFactory>> sslContextFactoryCustomizers;
    private List<Consumer<HttpClient>> httpClientCustomizers;

    // The standard implementation tends to force this to false, we have been using true, so we expose in our own builder.
    protected boolean cleanupExecutor;

    // Supported both via builder and TLSProvider injection concept of Steven's - used in code mostly only via TLSProvider
    // This would have to be revamped prior to contribution
    protected KeyStore truststore;
    protected KeyStore clientKeyStore;
    protected String clientPrivateKeyPassword;

    // Supported in builder, but never passed to the JettyClientEngine that Steven contributed to RestEasy
    // Note that that engine throws exception if you set it, we'd have to submit a PR to RestEasy to support
    protected HostnameVerificationPolicy policy = HostnameVerificationPolicy.WILDCARD;
    protected HostnameVerifier verifier = null;

    // Supported by builder, not used in JaxRSClientFactoryImpl (so default implementation is used)
    protected SSLContext sslContext;
    protected boolean disableTrustManager;
    protected ResteasyProviderFactory providerFactory;
    protected int responseBufferSize;
    // builder supported, not used in JaxRSClientFactoryImpl. This will cause problems with SSEEvents and such, but it's not very clear
    // when to expose.
    protected ScheduledExecutorService scheduledExecutorService;

    // builder supported, and set in JaxRSClientFactoryImpl
    protected ExecutorService asyncExecutor;
    private int proxyPort = -1;
    private String proxyHost = null;
    private String proxyScheme = null;
    // mapped to connectTimeout
    protected long establishConnectionTimeout = 15000L; // match jetty defaults
    protected TimeUnit establishConnectionTimeoutUnits = TimeUnit.MILLISECONDS;
    // mapped to defaultMaxConnectionsPerRoute
    protected int maxPooledPerRoute = 64; // match jetty defaults

    // Used in build() but seemingly unhooked to outside world, (which mirrors the odd implementation in RestEasyClientBuilderImpl!
    // I think it's meant as an arbitrary property to be passed to clientengine.builder
    protected Map<String, Object> properties = new HashMap<>();

    // Preserved part of the spirit of the builder, but it's still all or nothing. Core HttpCLient43 adds a shim builder, which copies
    // over other settings. Here, if you override, it's all or nothing - you own the whole shebbang. That could be addressed with
    // PR to RestEasy to enhance Steven's engine to copy over other settings.
    protected ClientHttpEngine httpEngine;

    // Builder supports, but it's not used - there's no clear Jetty equivalent.
    // Jetty has an idleTimeout, which is individually supported
    protected int connectionPoolSize = 50;
    protected long connectionTTL = -1;
    protected TimeUnit connectionTTLUnit = TimeUnit.MILLISECONDS;
    protected long socketTimeout = -1;
    protected TimeUnit socketTimeoutUnits = TimeUnit.MILLISECONDS;
    protected int connectionCheckoutTimeoutMs = -1;
    private boolean trustSelfSignedCertificates;

    // Not currently supported, could be with some investigation
    protected List<String> sniHostNames = new ArrayList<>();

    @Override
    public ResteasyClient build() {
        if (asyncExecutor == null) {
            asyncExecutor = Executors.newCachedThreadPool();
        }
        final HttpClient client = createHttpClient(
                httpClientCustomizers == null ? new ArrayList<>() : httpClientCustomizers,
                sslContextFactoryCustomizers == null ? new ArrayList<>() : sslContextFactoryCustomizers
        );
        final ClientConfiguration cc = new ClientConfiguration(getProviderFactory());
        properties.forEach(cc::property);

        final ClientHttpEngine clientHttpEngine = this.httpEngine == null ? new JettyClientEngine(client) : httpEngine;
        return new JettyRestEasyClient(clientHttpEngine, asyncExecutor, cleanupExecutor, scheduledExecutorService, cc);
    }

    public static class JettyRestEasyClient extends ResteasyClientImpl {

        protected JettyRestEasyClient(final ClientHttpEngine httpEngine, final ExecutorService asyncInvocationExecutor, final boolean cleanupExecutor, final ScheduledExecutorService scheduledExecutorService, final ClientConfiguration configuration) {
            super(httpEngine, asyncInvocationExecutor, cleanupExecutor, scheduledExecutorService, configuration);
        }
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

        return factory;
    }

    private HttpClient createHttpClient(List<Consumer<HttpClient>> httpClientCustomizers, List<Consumer<SslContextFactory>> sslContextFactoryCustomizers) {
        final HttpClient hc = new HttpClient(createSslFactory(sslContextFactoryCustomizers));
        // Don't let get below Jetty's recommended default
        hc.setMaxConnectionsPerDestination(Math.max(64, this.maxPooledPerRoute));
        hc.setExecutor(asyncExecutor);
        hc.setConnectTimeout(TimeUnit.MILLISECONDS.convert(establishConnectionTimeout, establishConnectionTimeoutUnits));
        hc.setAddressResolutionTimeout(TimeUnit.MILLISECONDS.convert(establishConnectionTimeout, establishConnectionTimeoutUnits));
        if (responseBufferSize > 0) {
            hc.setResponseBufferSize(responseBufferSize);
            hc.setRequestBufferSize(responseBufferSize);
        }
        if(StringUtils.isNotBlank(proxyHost) && proxyPort != 0) {
            HttpProxy proxy = new HttpProxy(proxyHost, proxyPort);
            hc.getProxyConfiguration().getProxies().add(proxy);
        }
        httpClientCustomizers.forEach(c -> c.accept(hc));
        return hc;
    }

    public JettyResteasyClientBuilder clientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public JettyResteasyClientBuilder httpClientCustomizers(final List<Consumer<HttpClient>> httpClientCustomizers) {
        this.httpClientCustomizers = httpClientCustomizers;
        return this;
    }

    public JettyResteasyClientBuilder sslContextFactoryCustomizers(List<Consumer<SslContextFactory>> factoryCustomizers) {
        this.sslContextFactoryCustomizers = factoryCustomizers;
        return this;
    }

    public JettyResteasyClientBuilder cleanupExecutor(boolean cleanupExecutor) {
        this.cleanupExecutor = cleanupExecutor;
        return this;
    }

    @Override
    public String toString() {
        return "JettyResteasyClientBuilder[" + clientName + "]";
    }

    @Override
    public String getDefaultProxyHostname() {
        return this.proxyHost;
    }

    @Override
    public int getDefaultProxyPort() {
        return this.proxyPort;
    }

    @Override
    public String getDefaultProxyScheme() {
        return this.proxyScheme;
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
        this.proxyHost = hostname;
        this.proxyPort = port;
        this.proxyScheme = scheme;
        return this;
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
    public ResteasyClientBuilder executorService(ExecutorService executorService)
    {
        this.asyncExecutor = executorService;
        return this;
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
    public boolean isTrustSelfSignedCertificates() {
        return this.trustSelfSignedCertificates;
    }

    @Override
    public void setIsTrustSelfSignedCertificates(boolean trustSelfSignedCertificates) {
        this.trustSelfSignedCertificates = trustSelfSignedCertificates;
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
        // Jetty is always async, so this is a no-op
        return this;
    }

    @Override
    public boolean isUseAsyncHttpEngine()
    {
        // Jetty is always async
        return httpEngine != null;
    }

    @Override
    public List<String> getSniHostNames()
    {
        return sniHostNames;
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
