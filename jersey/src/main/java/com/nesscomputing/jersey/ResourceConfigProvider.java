package com.nesscomputing.jersey;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Application;

import com.google.inject.Injector;

import org.glassfish.jersey.server.ResourceConfig;

class ResourceConfigProvider implements Provider<ResourceConfig>
{
    private static final String GUICE_INJECTOR_PROPERTY = "_GUICE_INJECTOR";

    private final Application application;
    private final Injector injector;

    @Inject
    ResourceConfigProvider(Application application, Injector injector)
    {
        this.application = application;
        this.injector = injector;
    }

    @Override
    public ResourceConfig get()
    {
        return ResourceConfig.forApplication(application).property(GUICE_INJECTOR_PROPERTY, injector);
    }
}
