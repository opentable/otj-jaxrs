package com.opentable.jaxrs;

import java.util.Collection;

import javax.ws.rs.client.Client;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class JaxRsClientModule extends AbstractModule
{
    public static final String CLIENT_PROPERTY = "ot.client.name";

    private final String name;
    private final Collection<JaxRsFeatureGroup> features;

    public JaxRsClientModule(String name, JaxRsFeatureGroup... features)
    {
        this.name = name;
        this.features = ImmutableSet.copyOf(features);
    }

    @Override
    protected void configure()
    {
        install (new JaxRsSharedModule());
        bind (Client.class).annotatedWith(Names.named(name)).toProvider(new JaxRsClientProvider(name, features));
    }
}
