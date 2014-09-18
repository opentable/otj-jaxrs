package com.opentable.jaxrs;

import com.google.inject.AbstractModule;

class JaxRsClientSharedModule extends AbstractModule {
    @Override
    public void configure() {
        bind (JaxRsClientBuilder.class).toProvider(JaxRsClientBuilder::newInstance);
    }
}
