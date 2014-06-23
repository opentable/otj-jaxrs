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
import javax.ws.rs.ext.RuntimeDelegate;

import com.google.common.collect.ImmutableSet;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

@Singleton
class JaxRsClientProvider implements Provider<Client>
{
    private final String name;
    private final Set<JaxRsFeatureGroup> featureGroups;

    private ResteasyProviderFactory delegate;
    private Map<JaxRsFeatureGroup, Feature> features;

    JaxRsClientProvider(String name, Collection<JaxRsFeatureGroup> featureGroups)
    {
        this.name = name;
        this.featureGroups = ImmutableSet.<JaxRsFeatureGroup>builder()
                .addAll(featureGroups)
                .add(PrivateFeatureGroup.WILDCARD)
                .build();
    }

    @Inject
    public void setRuntimeDelegate(RuntimeDelegate delegate)
    {
        this.delegate = (ResteasyProviderFactory) delegate;
    }

    @Inject
    public void setFeatures(Map<JaxRsFeatureGroup, Feature> features)
    {
        this.features = features;
    }

    @Override
    public Client get()
    {
        final ClientBuilder builder = ClientBuilder.newBuilder()
            .withConfig(delegate);

        for (Entry<JaxRsFeatureGroup, Feature> e : features.entrySet()) {
            if (featureGroups.contains(e.getKey())) {
                builder.register(e.getValue());
            }
        }

        return builder.property(JaxRsClientModule.CLIENT_PROPERTY, name)
            .build();
    }
}
