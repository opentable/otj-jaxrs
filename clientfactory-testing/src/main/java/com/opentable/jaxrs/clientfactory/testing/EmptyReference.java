package com.opentable.jaxrs.clientfactory.testing;

import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

import com.opentable.jaxrs.clientfactory.JaxRsClientBuilderImpl;

/**
 * Just a bunch of references to dependency classes to shut the dependency analyzer up.
 */
public final class EmptyReference
{
    // hack hack hack
    private static final ResteasyJackson2Provider provider = null;
    private static final JaxRsClientBuilderImpl builder = null;
    private EmptyReference() {}
}
