package com.nesscomputing.jersey;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

class ServletContainerProvider implements Provider<ServletContainer>
{
    private final ResourceConfig resourceConfig;
    private final Injector injector;

    @Inject
    ServletContainerProvider(ResourceConfig resourceConfig, Injector injector)
    {
        this.resourceConfig = resourceConfig;
        this.injector = injector;
    }

    @Override
    public ServletContainer get()
    {
        final ServletContainer servletContainer = new ServletContainer(resourceConfig);
        return servletContainer;
    }
}
