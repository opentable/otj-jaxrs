package com.opentable.jaxrs;

import com.google.inject.AbstractModule;

import org.jboss.resteasy.plugins.guice.ext.JaxrsModule;

final class JaxRsSharedModule extends AbstractModule
{
    @Override
    public void configure()
    {
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
}
