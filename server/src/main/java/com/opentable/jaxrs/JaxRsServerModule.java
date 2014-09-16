package com.opentable.jaxrs;

import com.google.inject.AbstractModule;
import org.jboss.resteasy.plugins.guice.ext.JaxrsModule;

public class JaxRsServerModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new JaxRsServerModule());
        install (new JaxrsModule());
    }
}
