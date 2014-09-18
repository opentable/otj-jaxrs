package com.opentable.jaxrs;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
class OTCorsFilter extends CorsFilter
{
    @Inject
    OTCorsFilter()
    {
        getAllowedOrigins().add("*");
    }
}
