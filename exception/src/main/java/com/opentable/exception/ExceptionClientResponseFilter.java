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
package com.opentable.exception;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercept exceptions that have been mapped to <code>x-ot/error</code> responses,
 * and rethrow them clientside.
 * Rudely consumes the http response body and never lets the actual response handler do anything.
 */
@Provider
@Deprecated
class ExceptionClientResponseFilter implements ClientResponseFilter
{
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionClientResponseFilter.class);
    private final ObjectMapper mapper;
    private final Map<String, Set<ExceptionReviver>> revivers;

    @Inject
    ExceptionClientResponseFilter(ObjectMapper mapper, Map<String, Set<ExceptionReviver>> revivers) {
        this.mapper = mapper;
        this.revivers = revivers;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException
    {
        final MediaType type = responseContext.getMediaType();
        if (type == null)
        {
            return;
        }

        if (type.isCompatible(OTApiException.MEDIA_TYPE))
        {
            final Map<String, Object> wrapper = mapper.readValue(responseContext.getEntityStream(), new TypeReference<Map<String, Object>>() {});
            final Object causes = wrapper.get("causes");

            Preconditions.checkState(causes instanceof List, "bad causes");
            final List<?> causesList = (List<?>) causes;

            LOG.debug("Received error responses {}", Joiner.on('\t').join(causesList));

            Preconditions.checkState(causesList.get(0) instanceof Map, "bad cause");

            final OTApiException exn = toException((Map<String, Object>) causesList.get(0));

            if (causesList.size() > 1) {
                LOG.debug("Multi-exception found.  first exception, remainder following.", exn);
            }

            for (int i = 1; i < causesList.size(); i++)
            {
                final OTApiException suppressed = toException((Map<String, Object>) causesList.get(i));
                LOG.debug("Multiple exceptions, continuation from prior backtrace...", suppressed);
                exn.addSuppressed(suppressed);
            }

            throw exn;
        }
    }

    private OTApiException toException(final Map<String, Object> fields)
    {
        final String type = Objects.toString(fields.get(OTApiException.ERROR_TYPE));

        final Set<ExceptionReviver> set = revivers.get(type);
        if (set.isEmpty())
        {
            LOG.error("Unknown exception type '{}'", type);
            return makeUnknownException(fields);
        }
        for (final ExceptionReviver er : set) {
            final OTApiException ex = er.apply(fields);
            if (ex != null)
            {
                return ex;
            }
        }
        LOG.error("No registered handler handled {}", fields);
        return makeUnknownException(fields);
    }

    private OTApiException makeUnknownException(Map<String, Object> fields)
    {
        return new UnknownOTApiException(fields);
    }
}
