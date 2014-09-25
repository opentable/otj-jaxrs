package com.opentable.jaxrs;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

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
