package com.opentable.jaxrs;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;

@Singleton
public class JaxRsClientProvider implements Provider<Client>
{
    @Override
    public Client get()
    {
        throw new UnsupportedOperationException(); // TODO
    }
}
