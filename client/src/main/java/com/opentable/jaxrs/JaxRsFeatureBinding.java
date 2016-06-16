package com.opentable.jaxrs;

import javax.ws.rs.core.Feature;

public class JaxRsFeatureBinding {
    private final JaxRsFeatureGroup group;
    private final Feature feature;

    public JaxRsFeatureBinding(JaxRsFeatureGroup group, Feature feature) {
        this.group = group;
        this.feature = feature;
    }

    public static JaxRsFeatureBinding bind(JaxRsFeatureGroup group, Feature feature) {
        return new JaxRsFeatureBinding(group, feature);
    }

    JaxRsFeatureGroup getGroup() {
        return group;
    }

    Feature getFeature() {
        return feature;
    }
}
