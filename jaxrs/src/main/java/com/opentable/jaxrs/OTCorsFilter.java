package com.opentable.jaxrs;

import javax.ws.rs.ext.Provider;

import javax.inject.Inject;
import com.google.inject.Singleton;

import org.jboss.resteasy.plugins.interceptors.CorsFilter;

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
