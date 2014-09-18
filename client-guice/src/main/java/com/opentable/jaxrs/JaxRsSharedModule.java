package com.opentable.jaxrs;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import com.opentable.jaxrs.json.OTJacksonJsonProvider;

/**
 * Bindings that all client and server modules share.
 */
final class JaxRsSharedModule extends AbstractModule {
    @Override
    public void configure()
    {
        bind(OTJacksonJsonProvider.class);
        bind(StreamedJsonResponseConverter.class);
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
