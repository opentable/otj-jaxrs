package com.opentable.jaxrs.clientfactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.Test;
import org.mockito.Mockito;

public class ResteasyClientBuilderTest
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
        ResteasyClientBuilder underlying = Mockito.mock(ResteasyClientBuilder.class);
        Client client = builderWithMockResteasy(underlying)
                .socketTimeout(42, TimeUnit.SECONDS)
                .build();
        Mockito.verify(underlying).socketTimeout(42, TimeUnit.SECONDS);
    }

    @Test
    public void connectTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        ResteasyClientBuilder underlying = Mockito.mock(ResteasyClientBuilder.class);
        Client client = builderWithMockResteasy(underlying)
                .connectTimeout(99, TimeUnit.SECONDS)
                .build();
        Mockito.verify(underlying).establishConnectionTimeout(99, TimeUnit.SECONDS);
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
