package com.opentable.jaxrs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Feature;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import com.opentable.config.Config;
import com.opentable.logging.Log;

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
    /** Client property that holds a {@code List<JaxRsFeatureGroup}. */
    public static final String FEATURE_GROUP_PROPERTY = "ot.jaxrs.feature-groups";

    private static final Log LOG = Log.findLog();

    private final Config config;

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
    @Inject
    public JaxRsClientFactory(Config config) {
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
    @Inject
    public synchronized JaxRsClientFactory addFeatureMap(Map<JaxRsFeatureGroup, Set<Feature>> map) {
        map.forEach((g, fs) -> fs.forEach(f -> addFeatureToGroup(g, f)));
        return this;
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
            LOG.trace("Group %s registers feature %s", group, f);
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
            LOG.trace("Group %s registers feature %s", group, f);
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

        final ClientBuilder builder = InternalClientFactoryHolder.newBuilder(jaxRsConfig);

        builder.property(CLIENT_NAME_PROPERTY, clientName);
        builder.property(FEATURE_GROUP_PROPERTY, featureGroups);

        LOG.debug("Building client '%s' with feature groups %s and config '%s'", clientName, featureGroups, jaxRsConfig);

        for (JaxRsFeatureGroup group : featureGroups) {
            for (Feature f : featureMap.get(group)) {
                LOG.trace("Client '%s' enabling feature %s", clientName, f);
                builder.register(f);
            }
        }

        return builder;
    }

    @VisibleForTesting
    protected JaxRsClientConfig configForClient(String clientName) {
        return config.getBean(
                JaxRsClientConfig.class,
                Collections.singletonMap("clientName", clientName));
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
}
