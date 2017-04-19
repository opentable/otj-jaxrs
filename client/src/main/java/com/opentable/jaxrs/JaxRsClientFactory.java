/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.jaxrs;

import static com.opentable.jaxrs.InternalClientFactoryHolder.factory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry for creating JAX-RS Clients.
 *
 * <p>This factory centralizes configuration and registration of JAX-RS extensions
 * through the {@link Feature} abstraction.  After the factory is constructed, you may
 * register features.  Configuration is declared by the {@link JaxRsClientConfig} class.
 *
 * <p>Each client is given a {@code clientName} and a list of {@link JaxRsFeatureGroup}s.
 * Feature groups provide a way to indirectly register features in a many-to-many relationship.
 * For example, a client library may wish to register for Discovery and RequestId forwarding
 * without understanding exactly which libraries perform these tasks.
 *
 * <p>Once you start to create Clients, you may no longer register new Features. Constructed
 * client objects are yours to own, and especially must be closed cleanly at shutdown
 * or they may leak significant resources.  Look at the Guice or Spring bindings if you
 * want a "batteries included" way of handling this.
 */
@Singleton
public class JaxRsClientFactory {
    /** Client property that holds the client name as provided to the factory. */
    public static final String CLIENT_NAME_PROPERTY = "ot.jaxrs.client-name";
    /** Client property that holds a {@code List<JaxRsFeatureGroup>}. */
    public static final String FEATURE_GROUP_PROPERTY = "ot.jaxrs.feature-groups";

    private static final Logger LOG = LoggerFactory.getLogger(JaxRsClientFactory.class);

    private final Function<String, JaxRsClientConfig> config;

    @GuardedBy("this")
    private final Multimap<JaxRsFeatureGroup, Feature> featureMap = HashMultimap.create();
    @GuardedBy("this")
    private final Multimap<JaxRsFeatureGroup, Class<Feature>> classFeatureMap = HashMultimap.create();

    @GuardedBy("this")
    private boolean started;

    /**
     * Initialize a new factory.  This factory is intended to be a singleton and should be
     * created once during application startup.
     */
    public JaxRsClientFactory() {
        this(name -> JaxRsClientConfig.DEFAULT);
    }

    /**
     * Initialize a new factory.  This factory is intended to be a singleton and should be
     * created once during application startup.
     */
    public JaxRsClientFactory(Function<String, JaxRsClientConfig> config) {
        this.config = config;
    }

    /**
     * Register many features at once.  Mostly a convenience for DI environments.
     */
    public synchronized JaxRsClientFactory addFeatureMap(SetMultimap<JaxRsFeatureGroup, Feature> map) {
        return addFeatureMap(Multimaps.asMap(map));
    }

    /**
     * Register many features at once.  Mostly a convenience for DI environments.
     */
    public synchronized JaxRsClientFactory addFeatureMap(Map<JaxRsFeatureGroup, Set<Feature>> map) {
        map.forEach((g, fs) -> fs.forEach(f -> addFeatureToGroup(g, f)));
        return this;
    }

    @Inject
    void injectBindings(Collection<JaxRsFeatureBinding> bindings) {
        bindings.forEach(b -> {
            if (b.getGroup() != null) {
                addFeatureToGroup(b.getGroup(), b.getFeature());
            } else {
                addFeatureToAllClients(b.getFeature());
            }
        });
    }

    /**
     * Register a list of features for all created clients.
     */
    public synchronized JaxRsClientFactory addFeatureToAllClients(Feature... features) {
        return addFeatureToGroup(PrivateFeatureGroup.WILDCARD, features);
    }

    /**
     * Register a list of features to all clients marked with the given group.
     */
    public synchronized JaxRsClientFactory addFeatureToGroup(JaxRsFeatureGroup group, Feature... features) {
        Preconditions.checkState(!started, "Already started building clients");
        featureMap.putAll(group, Arrays.asList(features));

        for (Feature f : features) {
            LOG.trace("Group {} registers feature {}", group, f);
        }
        return this;
    }

    /**
     * Register a list of features for all created clients.
     */
    @SafeVarargs
    public final synchronized JaxRsClientFactory addFeatureToAllClients(Class<Feature>... features) {
        return addFeatureToGroup(PrivateFeatureGroup.WILDCARD, features);
    }

    /**
     * Register a list of features to all clients marked with the given group.
     */
    @SafeVarargs
    public final synchronized JaxRsClientFactory addFeatureToGroup(JaxRsFeatureGroup group, Class<Feature>... features) {
        Preconditions.checkState(!started, "Already started building clients");
        classFeatureMap.putAll(group, Arrays.asList(features));

        for (Class<Feature> f : features) {
            LOG.trace("Group {} registers feature {}", group, f);
        }
        return this;
    }

    /**
     * Create a new {@link ClientBuilder} instance with the given name and groups.
     * You own the returned client and are responsible for managing its cleanup.
     */
    public synchronized ClientBuilder newBuilder(String clientName, Collection<JaxRsFeatureGroup> featureGroupsIn) {
        started = true;

        final JaxRsClientConfig jaxRsConfig = configForClient(clientName);

        final List<JaxRsFeatureGroup> featureGroups = ImmutableList.<JaxRsFeatureGroup>builder()
                .add(PrivateFeatureGroup.WILDCARD)
                .addAll(featureGroupsIn)
                .build();

        final ClientBuilder builder = factory().newBuilder(clientName, jaxRsConfig);

        builder.property(CLIENT_NAME_PROPERTY, clientName);
        builder.property(FEATURE_GROUP_PROPERTY, featureGroups);

        final List<Feature> features = featureGroups.stream()
            .flatMap(g -> featureMap.get(g).stream())
            .collect(Collectors.toList());

        LOG.debug("Building client '{}' with feature groups {}, features {}, and config '{}'", clientName, featureGroups, features, jaxRsConfig);
        features.forEach(builder::register);

        return builder;
    }

    @VisibleForTesting
    protected JaxRsClientConfig configForClient(String clientName) {
        return config.apply(clientName);
    }

    /**
     * Create a new {@link ClientBuilder} instance with the given name and groups.
     * You own the returned client and are responsible for managing its cleanup.
     */
    public synchronized ClientBuilder newBuilder(String clientName, JaxRsFeatureGroup feature, JaxRsFeatureGroup... moreFeatures) {
        return newBuilder(clientName, ImmutableList.<JaxRsFeatureGroup>builder()
                .add(feature)
                .addAll(Arrays.asList(moreFeatures))
                .build());
    }

    /**
     * Create a new {@link Client} instance with the given name and groups.
     * You own the returned client and are responsible for managing its cleanup.
     */
    public Client newClient(String clientName, JaxRsFeatureGroup feature, JaxRsFeatureGroup... moreFeatures) {
        return newBuilder(clientName, feature, moreFeatures).build();
    }

    /**
     * Create a new {@link Client} instance with the given name and groups.
     * You own the returned client and are responsible for managing its cleanup.
     */
    public Client newClient(String clientName, Collection<JaxRsFeatureGroup> featureGroups) {
        return newBuilder(clientName, featureGroups).build();
    }

    /**
     * Create a Client proxy for the given interface type.
     * Note that different JAX-RS providers behave slightly
     * differently for this feature.
     *
     * @param proxyClass the class to implement
     * @param baseTarget the API root
     * @return a proxy implementation that executes requests
     */
    public <T> T createClientProxy(Class<T> proxyClass, WebTarget baseTarget) {
        return factory().createClientProxy(proxyClass, baseTarget);
    }
}
