package com.opentable.jaxrs;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.opentable.jaxrs.json.OTJacksonJsonProvider;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

class ClientJsonFeature implements Feature
{
    private final OTJacksonJsonProvider custom;
    private final JacksonJsonProvider standard;

    @Inject
    ClientJsonFeature(OTJacksonJsonProvider custom, JacksonJsonProvider standard)
    {
        this.custom = custom;
        this.standard = standard;
    }

    @Override
    public boolean configure(FeatureContext context)
    {
        context.register(custom);
        context.register(standard);
        return true;
    }
}
