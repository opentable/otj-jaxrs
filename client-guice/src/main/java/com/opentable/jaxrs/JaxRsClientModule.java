package com.opentable.jaxrs;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

import com.opentable.lifecycle.Lifecycle;
import com.opentable.lifecycle.LifecycleStage;

/**
 * Bind a {@link JaxRsClientFactory} created {@link Client} to the configuration and lifecycle
 * in a Guice environment.
 */
public class JaxRsClientModule extends AbstractModule
{
    public static final String CLIENT_PROPERTY = "ot.client.name";

    private final String name;
    private final Collection<JaxRsFeatureGroup> features;

    public JaxRsClientModule(String name, JaxRsFeatureGroup feature, JaxRsFeatureGroup... moreFeatures)
    {
        this.name = name;
        this.features = ImmutableSet.<JaxRsFeatureGroup>builder()
                .add(feature)
                .addAll(Arrays.asList(moreFeatures))
                .build();
    }

    @Override
    protected void configure()
    {
        install (new JaxRsClientSharedModule());
        install (new JaxRsSharedModule());
        bind (Client.class).annotatedWith(Names.named(name)).toProvider(new ClientProvider(name, features)).in(Scopes.SINGLETON);
    }

    @Singleton
    private static class ClientProvider implements Provider<Client> {
        private final String name;
        private final Collection<JaxRsFeatureGroup> features;

        private JaxRsClientFactory factory;
        private Lifecycle lifecycle;

        public ClientProvider(String name, Collection<JaxRsFeatureGroup> features) {
            this.name = name;
            this.features = features;
        }

        @Inject
        public void inject(JaxRsClientFactory factory, Lifecycle lifecycle) {
            this.factory = factory;
            this.lifecycle = lifecycle;
        }

        @Override
        public Client get() {
            final Client client = factory.newClient(name, features);
            lifecycle.addListener(LifecycleStage.STOP_STAGE, client::close);
            return client;
        }
    }
}
