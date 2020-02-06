package com.opentable.jaxrs.referrer;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.springframework.context.annotation.Import;

import com.opentable.service.ReferringInformation;

@Import({ClientReferrerFilter.class, ReferringInformation.class})
public class ClientReferrerFeature implements Feature {
    private final ClientReferrerFilter filter;

    @Inject
    public ClientReferrerFeature(final ClientReferrerFilter filter) {
        this.filter = filter;
    }

    @Override
    public boolean configure(final FeatureContext context) {
        final boolean active = filter.isActive();
        if (active) {
            context.register(filter);
        }
        return active;
    }
}
