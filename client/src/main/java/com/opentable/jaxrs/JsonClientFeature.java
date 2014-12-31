/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
