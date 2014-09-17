package com.opentable.jaxrs.clientfactory;

import java.io.IOException;

import javax.ws.rs.client.Client;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

/**
 * The RESTEasy implementation of ClientFactory. Hides RESTEasy specific stuff
 * behind a common facade.
 */
public class JaxRsClientBuilderImpl implements JaxRsClientBuilder
{
    private final ResteasyClientBuilder clientBuilder = getResteasyClientBuilder();
    private JaxRsClientConfig config;

    @Override
    public JaxRsClientBuilder register(Object object)
    {
        clientBuilder.register(object);
        return this;
    }

    @Override
    public JaxRsClientBuilder register(Class<?> clazz)
    {
        clientBuilder.register(clazz);
        return this;
    }

    @Override
    public JaxRsClientBuilder withConfiguration(JaxRsClientConfig config)
    {
        this.config = config;
        configureHttpEngine(config);
        configureAuthenticationIfNeeded(config);
        return this;
    }

    @Override
    public Client build()
    {
        return clientBuilder.build();
    }

    /** package-public, used in test only */
    ResteasyClientBuilder getResteasyClientBuilder()
    {
        return new ResteasyClientBuilder();
    }

    private void configureHttpEngine(JaxRsClientConfig config)
    {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        if (config.isEtcdHacksEnabled()) {
            builder
                .setRedirectStrategy(new ExtraLaxRedirectStrategy())
                .addInterceptorFirst(new SwallowHeaderInterceptor(HttpHeaders.CONTENT_LENGTH));
        }
        final HttpClient client = builder
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout((int) config.socketTimeout().getMillis())
                        .build())
                .setDefaultRequestConfig(customRequestConfig(config, RequestConfig.custom()))
                .setMaxConnTotal(config.httpClientMaxTotalConnections())
                .setMaxConnPerRoute(config.httpClientDefaultMaxPerRoute())
                .build();
        final ApacheHttpClient4Engine engine = new HackedApacheHttpClient4Engine(config, client);
        clientBuilder.httpEngine(engine);
    }

    private static RequestConfig customRequestConfig(JaxRsClientConfig config, RequestConfig.Builder base) {
        base.setRedirectsEnabled(true);
        if (config != null) {
            base.setConnectionRequestTimeout((int) config.connectionPoolTimeout().getMillis())
                .setConnectTimeout((int) config.connectTimeout().getMillis())
                .setSocketTimeout((int) config.socketTimeout().getMillis());
        }
        return base.build();
    }

    private void configureAuthenticationIfNeeded(JaxRsClientConfig config)
    {
        if (!StringUtils.isEmpty(config.basicAuthUserName()) && !StringUtils.isEmpty(config.basicAuthPassword()))
        {
            final BasicAuthentication auth = new BasicAuthentication(
                    config.basicAuthUserName(), config.basicAuthPassword());
            clientBuilder.register(auth);
        }
    }

    private static class HackedApacheHttpClient4Engine extends ApacheHttpClient4Engine {
        private final JaxRsClientConfig config;

        HackedApacheHttpClient4Engine(JaxRsClientConfig config, HttpClient client) {
            super(client);
            this.config = config;
        }

        @Override
        protected HttpRequestBase createHttpMethod(String url, String restVerb) {
            final HttpRequestBase result = super.createHttpMethod(url, restVerb);
            final Builder base = result.getConfig() == null ? RequestConfig.custom() : RequestConfig.copy(result.getConfig());
            result.setConfig(customRequestConfig(config, base));
            return result;
        }
    }

    private static class SwallowHeaderInterceptor implements HttpRequestInterceptor {
        private final String[] headers;

        SwallowHeaderInterceptor(String... headers) {
            this.headers = headers;
        }

        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            for (String header : headers) {
                request.removeHeaders(header);
            }
        }
    }
}
