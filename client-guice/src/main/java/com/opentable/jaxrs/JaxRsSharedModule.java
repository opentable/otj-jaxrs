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

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import com.opentable.jaxrs.json.OTJacksonJsonProvider;

/**
 * Bindings that all client and server modules share.
 */
final class JaxRsSharedModule extends AbstractModule {
    @Override
    public void configure()
    {
        bind(OTJacksonJsonProvider.class);
        bind(StreamedJsonResponseConverter.class);
    }

    @Override
    public int hashCode()
    {
        return JaxRsSharedModule.class.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof JaxRsSharedModule;
    }

    /**
     * Binds the JacksonJsonProvider to JAX-RS.
     */
    @Provides
    @Singleton
    JacksonJsonProvider getJacksonJsonProvider(final ObjectMapper objectMapper)
    {
        final JacksonJsonProvider provider = new JacksonJsonProvider();
        provider.setMapper(objectMapper);
        return provider;
    }
}
