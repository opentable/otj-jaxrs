package com.opentable.jaxrs;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

import javax.ws.rs.core.Feature;

public class JaxRsClientBinder {

    public static LinkedBindingBuilder<Feature> bindFeatureForAllClients(final Binder binder)
    {
        return MapBinder.newMapBinder(binder, JaxRsFeatureGroup.class, Feature.class).permitDuplicates().addBinding(PrivateFeatureGroup.WILDCARD);
    }
}
