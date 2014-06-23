package com.opentable.jaxrs;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.servlet.ServletModule;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kitei.testing.lessio.AllowNetworkAccess;
import org.kitei.testing.lessio.AllowNetworkListen;

import com.opentable.config.Config;
import com.opentable.config.ConfigModule;
import com.opentable.httpserver.HttpServerModule;
import com.opentable.lifecycle.Lifecycle;
import com.opentable.lifecycle.LifecycleStage;
import com.opentable.lifecycle.guice.LifecycleModule;
import com.opentable.server.PortNumberProvider;

@AllowNetworkListen(ports= {0})
@AllowNetworkAccess(endpoints= {"127.0.0.1:*"})
public class TestLZ4Encoding
{
    private static final String BIG_CONTENT_RESOURCE = "/test-resources/big-content.txt";

    @Inject
    private Lifecycle lifecycle = null;

    @Inject
    private PortNumberProvider pnp;

    private String baseUri = null;

    @Before
    public void setUp() throws Exception
    {
        final Config config = Config.getEmptyConfig();

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       new ConfigModule(config),
                                                       new HttpServerModule(config),
                                                       new LifecycleModule(),
                                                       new ServletModule() {
                                                            @Override
                                                            public void configureServlets() {
                                                                binder().requireExplicitBindings();
                                                                binder().disableCircularProxies();

                                                                bind (ContentServlet.class);
                                                                serve("/content").with(ContentServlet.class);
                                                            }
                                                       });

        injector.injectMembers(this);
        lifecycle.executeTo(LifecycleStage.START_STAGE);
        baseUri = format("http://localhost:%d/", pnp.getPort());
    }

    @Singleton
    static class ContentServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        private ServletConfig config;

        @Override
        public void init(ServletConfig configIn) throws ServletException
        {
            this.config = configIn;
        }

        @Override
        public ServletConfig getServletConfig()
        {
            return config;
        }

        @Override
        public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
        {
            res.setStatus(HttpStatus.OK_200);
            res.setContentType(MediaType.PLAIN_TEXT_UTF_8.toString());
            IOUtils.copy(TestLZ4Encoding.class.getResourceAsStream(BIG_CONTENT_RESOURCE), res.getOutputStream());
        }

        @Override
        public String getServletInfo()
        {
            return "Content Servlet";
        }

        @Override
        public void destroy()
        {
        }
    }

    @After
    public void teardown()
    {
        lifecycle.executeTo(LifecycleStage.STOP_STAGE);
    }

    private static final String BIG_CONTENT;

    static {
        try {
            BIG_CONTENT = IOUtils.toString(TestLZ4Encoding.class.getResourceAsStream(BIG_CONTENT_RESOURCE));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    public void testGzip() throws Exception
    {
        final Response content = ClientBuilder.newClient().target(baseUri + "/content")
                .request().header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .get();

        Assert.assertEquals(HttpStatus.OK_200, content.getStatus());
        Assert.assertEquals("gzip", content.getHeaderString(HttpHeaders.CONTENT_ENCODING));

        String data = IOUtils.toString(content.readEntity(InputStream.class), Charsets.UTF_8);
        Assert.assertEquals(BIG_CONTENT, data);
    }

    @Test
    public void testLZ4() throws Exception
    {
        final Response content = ClientBuilder.newClient().register(LZ4DecodingInterceptor.class)
                .target(baseUri + "/content")
                .request().header(HttpHeaders.ACCEPT_ENCODING, "lz4")
                .get();

        Assert.assertEquals(HttpStatus.OK_200, content.getStatus());
        Assert.assertEquals("lz4", content.getHeaderString(HttpHeaders.CONTENT_ENCODING));

        String data = IOUtils.toString(content.readEntity(InputStream.class), Charsets.UTF_8);
        Assert.assertEquals(BIG_CONTENT, data);
    }
}
