package com.opentable.jaxrs;

import javax.ws.rs.ext.Provider;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;

import javax.ws.rs.container.PreMatching;

@Singleton
@Provider
@PreMatching
class OTCorsFilter extends CorsFilter
{
    @Inject
    OTCorsFilter()
    {
        getAllowedOrigins().add("*");
    }
}
