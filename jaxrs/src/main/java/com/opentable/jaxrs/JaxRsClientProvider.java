package com.opentable.jaxrs;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Feature;

import com.google.common.collect.ImmutableSet;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.opentable.lifecycle.Lifecycle;
import com.opentable.lifecycle.LifecycleStage;

@Singleton
class JaxRsClientProvider implements Provider<Client>
{
    private final String name;
    private final Set<JaxRsFeatureGroup> featureGroups;

    private Map<JaxRsFeatureGroup, Set<Feature>> features;
    private Lifecycle lifecycle;

    JaxRsClientProvider(String name, Collection<JaxRsFeatureGroup> featureGroups)
    {
        this.name = name;
        this.featureGroups = ImmutableSet.<JaxRsFeatureGroup>builder()
                .addAll(featureGroups)
                .add(PrivateFeatureGroup.WILDCARD)
                .build();
    }

    @Inject
    public void setFeatures(Map<JaxRsFeatureGroup, Set<Feature>> features)
    {
        this.features = features;
    }

    @Inject
    public void setLifecycle(Lifecycle lifecycle)
    {
        this.lifecycle = lifecycle;
    }

    @Override
    public Client get()
    {
        final ClientBuilder builder = ClientBuilder.newBuilder();

        for (Entry<JaxRsFeatureGroup, Set<Feature>> e : features.entrySet()) {
            JaxRsFeatureGroup group = e.getKey();
            if (featureGroups.contains(group)) {
                for (Feature f : e.getValue()) {
                    builder.register(f);
                }
            }
        }

        builder.property(JaxRsClientModule.CLIENT_PROPERTY, name);
        ((ResteasyClientBuilder) builder).connectionPoolSize(40);
        final Client client = builder.build();
        lifecycle.addListener(LifecycleStage.STOP_STAGE, client::close);
        return client;
    }
}
