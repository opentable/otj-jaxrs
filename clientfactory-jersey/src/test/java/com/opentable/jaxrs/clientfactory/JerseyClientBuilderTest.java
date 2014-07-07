package com.opentable.jaxrs.clientfactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

import org.junit.Test;

public class JerseyClientBuilderTest
{
    @Test
    public void instanceCreatesBuilderImpls()
    {
        JaxRsClientBuilder builder = JaxRsClientBuilder.instance();
        assertEquals(JaxRsClientBuilderImpl.class, builder.getClass());
    }

    @Test
    public void builderImplsImplementBuilder()
    {
        JaxRsClientBuilder builder = JaxRsClientBuilder.instance();
        assertTrue(JaxRsClientBuilder.class.isAssignableFrom(builder.getClass()));
    }
    
    @Test
    public void socketTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        Client client = JaxRsClientBuilder.instance()
                .socketTimeout(42, TimeUnit.SECONDS)
                .build();
        String result = client.getConfiguration().getProperty("jersey.config.client.readTimeout").toString();
        assertEquals("42000", result);
    }

    @Test
    public void connectTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        Client client = JaxRsClientBuilder.instance()
                .connectTimeout(99, TimeUnit.SECONDS)
                .build();
        String result = client.getConfiguration().getProperty("jersey.config.client.connectTimeout").toString();
        assertEquals("99000", result);
    }



}
