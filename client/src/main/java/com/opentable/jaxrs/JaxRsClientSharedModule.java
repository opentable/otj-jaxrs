package com.opentable.jaxrs;

import com.google.inject.AbstractModule;

import com.opentable.jaxrs.clientfactory.JaxRsClientBuilder;

class JaxRsClientSharedModule extends AbstractModule {
    @Override
    public void configure() {
        bind (JaxRsClientBuilder.class).toProvider(JaxRsClientBuilder::newInstance);
    }
}
