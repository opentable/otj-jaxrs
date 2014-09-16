package com.opentable.jaxrs;

import com.google.common.collect.ImmutableSet;
import com.opentable.jaxrs.clientfactory.JaxRsClientBuilder;
import com.opentable.jaxrs.clientfactory.JaxRsClientConfig;
import com.opentable.lifecycle.Lifecycle;
import com.opentable.lifecycle.LifecycleStage;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Feature;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Singleton
class JaxRsClientProvider implements Provider<Client>
{
    private final String name;
    private final Set<JaxRsFeatureGroup> featureGroups;
    private JaxRsClientConfig clientConfig;

    private Map<JaxRsFeatureGroup, Set<Feature>> features;
    private Lifecycle lifecycle;
    private JaxRsClientBuilder clientBuilder;

    JaxRsClientProvider(String name, Collection<JaxRsFeatureGroup> featureGroups)
    {
        this.name = name;
        this.featureGroups = ImmutableSet.<JaxRsFeatureGroup>builder()
                .addAll(featureGroups)
                .add(PrivateFeatureGroup.WILDCARD)
                .build();
    }

    @Inject
    public void setClientConfig(JaxRsClientConfig clientConfig) {
        this.clientConfig = clientConfig;
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

    @Inject
    public void setClientBuilder(JaxRsClientBuilder builder) {
        this.clientBuilder = builder;
    }

    @Override
    public Client get()
    {
        final JaxRsClientBuilder builder = clientBuilder.withConfiguration(clientConfig);

        for (Entry<JaxRsFeatureGroup, Set<Feature>> e : features.entrySet()) {
            final JaxRsFeatureGroup group = e.getKey();
            if (featureGroups.contains(group)) {
                for (Feature f : e.getValue()) {
                    builder.register(f);
                }
            }
        }

        final Client client = builder.build();
        client.property(JaxRsClientModule.CLIENT_PROPERTY, name);
        lifecycle.addListener(LifecycleStage.STOP_STAGE, client::close);
        return client;
    }
}
