package com.opentable.jaxrs.referrer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.httpheaders.OTHeaders;
import com.opentable.service.ReferringInformation;
import com.opentable.service.ServiceInfo;

@Provider
public class ClientReferrerFilter implements ClientRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ClientReferrerFilter.class);

    private final Map<String, List<Object>> referrerHeaders;

    @Inject
    public ClientReferrerFilter(final ReferringInformation referringInformation, final Optional<ServiceInfo> serviceInfo) {
        this(
                referringInformation,
                serviceInfo.map(ServiceInfo::getName).orElse(null)
        );
    }

    public ClientReferrerFilter(final ReferringInformation referringInformation, @Nullable final String serviceName) {
        final ImmutableMap.Builder<String, List<Object>> builder = ImmutableMap.builder();
        if (referringInformation.getReferringHost() != null) {
            builder.put(OTHeaders.REFERRING_HOST, ImmutableList.of(referringInformation.getReferringHost()));
        }
        if (referringInformation.getReferringEnvironment() != null) {
            builder.put(OTHeaders.REFERRING_ENV, ImmutableList.of(referringInformation.getReferringEnvironment()));
        }
        if (serviceName != null) {
            builder.put(OTHeaders.REFERRING_SERVICE, ImmutableList.of(serviceName));
        }
        referrerHeaders = builder.build();
        if (isActive()) {
            LOG.info("on outgoing jax-rs requests, will set headers: {}", referrerHeaders);
        } else {
            LOG.warn("no headers found for outgoing jax-rs requests");
        }
    }

    final boolean isActive() {
        return !referrerHeaders.isEmpty();
    }

    @Override
    public void filter(final ClientRequestContext requestContext) {
        LOG.trace("adding headers '{}' to request", referrerHeaders);
        requestContext.getHeaders().putAll(referrerHeaders);
    }
}
