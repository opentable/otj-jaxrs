package com.opentable.jaxrs;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/**
 * Bindings shared by all clients.
 */
final class JaxRsClientSharedModule extends AbstractModule {
    @Override
    public void configure() {
        bind (JaxRsClientFactory.class).in(Scopes.SINGLETON);
        JaxRsClientBinder.bindFeatureForAllClients(binder()).to(JsonClientFeature.class);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass() == obj.getClass();
    }
}
