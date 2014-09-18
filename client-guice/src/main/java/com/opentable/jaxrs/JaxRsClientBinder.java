package com.opentable.jaxrs;

import javax.ws.rs.core.Feature;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public final class JaxRsClientBinder {
    private JaxRsClientBinder() { }

    public static LinkedBindingBuilder<Feature> bindFeatureForAllClients(Binder binder) {
        return bindFeatureToGroup(binder, PrivateFeatureGroup.WILDCARD);
    }

    public static LinkedBindingBuilder<Feature> bindFeatureToGroup(Binder binder, JaxRsFeatureGroup group) {
        return MapBinder.newMapBinder(binder, JaxRsFeatureGroup.class, Feature.class)
                .permitDuplicates().addBinding(group);
    }
}
