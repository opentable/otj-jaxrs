package com.opentable.jaxrs.clientfactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.skife.config.TimeSpan;

import com.opentable.jaxrs.JaxRsClientBuilder;
import com.opentable.jaxrs.JaxRsClientConfig;

// It sucks that this test is ignored, but I have no idea how to test e.g. socket
// connect timeouts without a lot of harnessing...
@Ignore
public class ResteasyClientBuilderTest
{

    @Test
    public void instanceCreatesBuilderImpls()
    {
        JaxRsClientBuilder builder = JaxRsClientBuilder.newInstance();
        assertEquals(JaxRsClientBuilderImpl.class, builder.getClass());
    }

    @Test
    public void builderImplsImplementBuilder()
    {
        JaxRsClientBuilder builder = JaxRsClientBuilder.newInstance();
        assertTrue(JaxRsClientBuilder.class.isAssignableFrom(builder.getClass()));
    }

    @Test
    public void socketTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        final JaxRsClientConfig conf = mock(JaxRsClientConfig.class);
        when(conf.socketTimeout()).thenReturn(new TimeSpan(6600, TimeUnit.MILLISECONDS));
        final ResteasyClientBuilder underlying = mock(ResteasyClientBuilder.class);

        builderWithMockResteasy(underlying).withConfiguration(conf).build();

        Mockito.verify(underlying).socketTimeout(6600, TimeUnit.MILLISECONDS);
    }

    @Test
    public void connectTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        final JaxRsClientConfig conf = mock(JaxRsClientConfig.class);
        when(conf.connectTimeout()).thenReturn(new TimeSpan(4400, TimeUnit.MILLISECONDS));
        final ResteasyClientBuilder underlying = mock(ResteasyClientBuilder.class);

        builderWithMockResteasy(underlying).withConfiguration(conf).build();

        Mockito.verify(underlying).establishConnectionTimeout(4400, TimeUnit.MILLISECONDS);
    }

    private JaxRsClientBuilderImpl builderWithMockResteasy(final ResteasyClientBuilder mock)
    {
        return new JaxRsClientBuilderImpl() {
            @Override
            ResteasyClientBuilder getResteasyClientBuilder() {
                return mock;
            }
        };
    }
}
