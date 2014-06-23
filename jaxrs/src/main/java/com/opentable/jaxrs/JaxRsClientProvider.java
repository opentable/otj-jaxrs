package com.opentable.jaxrs;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@Singleton
class JaxRsClientProvider implements Provider<Client>
{
    private final String name;

    JaxRsClientProvider(String name)
    {
        this.name = name;
    }

    @Override
    public Client get()
    {
        ClientBuilder builder = ClientBuilder.newBuilder();
        builder.property(JaxRsClientModule.CLIENT_PROPERTY, name);
        return builder.build();
    }
}
