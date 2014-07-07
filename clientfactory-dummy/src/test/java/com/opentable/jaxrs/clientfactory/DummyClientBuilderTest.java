package com.opentable.jaxrs.clientfactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DummyClientBuilderTest
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
}
