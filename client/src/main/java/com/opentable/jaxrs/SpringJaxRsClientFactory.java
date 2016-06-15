package com.opentable.jaxrs;

import java.io.Closeable;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.client.Client;

import com.google.common.collect.ImmutableList;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class SpringJaxRsClientFactory implements FactoryBean<Client>, Closeable, EnvironmentAware {
    private final String clientName;
    private final List<JaxRsFeatureGroup> features;

    private Client client;
    //private Environment environment;

    private JaxRsClientFactory factory;

    public SpringJaxRsClientFactory(String clientName, JaxRsFeatureGroup... features) {
        this.clientName = clientName;
        this.features = ImmutableList.copyOf(features);
    }

    @Inject
    public SpringJaxRsClientFactory setJaxRsFactory(JaxRsClientFactory factory) {
        this.factory = factory;
        return this;
    }

    @Override
    @PreDestroy
    public synchronized void close() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public synchronized Client getObject() throws Exception {
        if (client == null) {
            client = createClient();
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return Client.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setEnvironment(Environment environment) {
        //this.environment = environment;
    }

    private Client createClient() {
        return factory.newClient(clientName, features);
    }
}
