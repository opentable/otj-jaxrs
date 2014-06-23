package com.opentable.jaxrs;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

@Singleton
class JaxRsClientProvider implements Provider<Client>
{
    private final String name;
    private ResteasyProviderFactory delegate;

    JaxRsClientProvider(String name)
    {
        this.name = name;
    }

    @Inject
    public void setRuntimeDelegate(RuntimeDelegate delegate)
    {
        this.delegate = (ResteasyProviderFactory) delegate;
    }

    @Override
    public Client get()
    {
        ClientBuilder builder = ClientBuilder.newBuilder();
        for (Class<?> c : delegate.getProviderClasses()) {
            builder.register(c);
        }
        for (Object o : delegate.getProviderInstances()) {
            builder.register(o);
        }
        builder.property(JaxRsClientModule.CLIENT_PROPERTY, name);
        return builder.build();
    }
}
