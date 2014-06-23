package com.opentable.exception;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

class ExceptionClientResponseFeature implements Feature
{
    private final ExceptionClientResponseFilter filter;

    @Inject
    ExceptionClientResponseFeature(ExceptionClientResponseFilter filter)
    {
        this.filter = filter;
    }

    @Override
    public boolean configure(FeatureContext context)
    {
        context.register(filter);
        return true;
    }
}
