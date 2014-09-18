package com.opentable.jaxrs;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import com.opentable.jaxrs.json.OTJacksonJsonProvider;

/**
 * Feature which enables Jackson JSON reading of {@code application/json}
 * for clients.
 */
public class JsonClientFeature implements Feature
{
    private final OTJacksonJsonProvider custom;
    private final JacksonJsonProvider standard;

    @Inject
    JsonClientFeature(OTJacksonJsonProvider custom, JacksonJsonProvider standard)
    {
        this.custom = custom;
        this.standard = standard;
    }

    public static JsonClientFeature forMapper(ObjectMapper mapper)
    {
        final JacksonJsonProvider provider = new JacksonJsonProvider(mapper);
        final OTJacksonJsonProvider otProvider = new OTJacksonJsonProvider(provider);
        return new JsonClientFeature(otProvider, provider);
    }

    @Override
    public boolean configure(FeatureContext context)
    {
        context.register(custom);
        context.register(standard);
        return true;
    }
}
