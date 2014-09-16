package com.opentable.jaxrs;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.client.Client;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import com.opentable.config.ConfigProvider;
import com.opentable.jaxrs.clientfactory.JaxRsClientConfig;

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
        final Annotation annotation = Names.named(name);

        bind (JaxRsClientConfig.class).annotatedWith(annotation).toProvider(
                ConfigProvider.of(JaxRsClientConfig.class,
                                  Collections.singletonMap("clientName", name)));

        install (new JaxRsClientSharedModule());
        install (new JaxRsSharedModule());
        bind (Client.class).annotatedWith(annotation).toProvider(new JaxRsClientProvider(name, features));
        JaxRsClientBinder.bindFeatureForAllClients(binder()).to(ClientJsonFeature.class);
    }
}
