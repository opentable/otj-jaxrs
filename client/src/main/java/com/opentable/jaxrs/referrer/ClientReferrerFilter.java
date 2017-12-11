package com.opentable.jaxrs.referrer;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.service.AppInfo;
import com.opentable.service.ServiceInfo;

@Provider
public class ClientReferrerFilter implements ClientRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ClientReferrerFilter.class);

    private final Map<String, List<Object>> referrerHeaders;

    @Inject
    public ClientReferrerFilter(final AppInfo appInfo, final ServiceInfo serviceInfo) {
        this(appInfo.getTaskHost(), serviceInfo.getName());
    }

    public ClientReferrerFilter(@Nullable final String host, final String serviceName) {
        final ImmutableMap.Builder<String, List<Object>> builder = ImmutableMap.builder();
        if (host != null) {
            builder.put(ClientReferrerConfiguration.HOST_HEADER_NAME, ImmutableList.of(host));
        }
        builder.put(ClientReferrerConfiguration.SERVICE_HEADER_NAME, ImmutableList.of(serviceName));
        referrerHeaders = builder.build();
        LOG.info("on outgoing jax-rs requests, will set headers: {}", referrerHeaders);
    }

    @Override
    public void filter(final ClientRequestContext requestContext) {
        LOG.trace("adding headers '{}' to request", referrerHeaders);
        requestContext.getHeaders().putAll(referrerHeaders);
    }
}
