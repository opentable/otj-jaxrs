package com.opentable.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.jboss.resteasy.plugins.guice.ext.JaxrsModule;

final class JaxRsSharedModule extends AbstractModule
{
    @Override
    public void configure()
    {
        JaxRsBinder.bindFeatureForAllClients(binder()).toInstance((g) -> true);
        install (new JaxrsModule());
    }

    @Override
    public int hashCode()
    {
        return JaxRsSharedModule.class.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof JaxRsSharedModule;
    }

    /**
     * Binds the JacksonJsonProvider to JAX-RS.
     */
    @Provides
    @Singleton
    JacksonJsonProvider getJacksonJsonProvider(final ObjectMapper objectMapper)
    {
        final JacksonJsonProvider provider = new JacksonJsonProvider();
        provider.setMapper(objectMapper);
        return provider;
    }
}
