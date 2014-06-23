package com.opentable.jaxrs;

import javax.ws.rs.client.Client;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class JaxRsClientModule extends AbstractModule
{
    public static final String CLIENT_PROPERTY = "ot.client.name";

    private final String name;

    public JaxRsClientModule(String name)
    {
        this.name = name;
    }

    @Override
    protected void configure()
    {
        bind (Client.class).annotatedWith(Names.named(name)).toProvider(new JaxRsClientProvider(name));
    }
}
