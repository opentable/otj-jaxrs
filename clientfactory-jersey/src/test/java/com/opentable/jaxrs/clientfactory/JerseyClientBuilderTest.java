package com.opentable.jaxrs.clientfactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

import org.junit.Test;
import org.skife.config.TimeSpan;

public class JerseyClientBuilderTest
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
        final Client client = JaxRsClientBuilder.newInstance().withConfiguration(conf).build();
        String result = client.getConfiguration().getProperty("jersey.config.client.readTimeout").toString();
        assertEquals("6600", result);
    }

    @Test
    public void connectTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        final JaxRsClientConfig conf = mock(JaxRsClientConfig.class);
        when(conf.connectTimeout()).thenReturn(new TimeSpan(4400, TimeUnit.MILLISECONDS));
        final Client client = JaxRsClientBuilder.newInstance().withConfiguration(conf).build();
        final String result = client.getConfiguration().getProperty("jersey.config.client.connectTimeout").toString();
        assertEquals("4400", result);
    }
}
