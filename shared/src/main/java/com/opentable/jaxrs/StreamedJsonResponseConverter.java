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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.callback.Callback;
import com.opentable.callback.CallbackRefusedException;

@Singleton
public class StreamedJsonResponseConverter
{
    private static final Logger LOG = LoggerFactory.getLogger(StreamedJsonResponseConverter.class);

    private final ObjectMapper mapper;

    @Inject
    StreamedJsonResponseConverter(ObjectMapper mapper)
    {
        this.mapper = mapper;
    }

    public <T> void read(Response response,
            Callback<T> callback,
            TypeReference<T> type)
    throws IOException
    {
        try {
            final int sc = response.getStatus();
            switch(sc)
            {
            case 201:
            case 204:
                LOG.debug("Return code is {}, finishing.", response.getStatus());
                return;
            case 200:
                try (JsonParser jp = mapper.getFactory().createParser(response.readEntity(InputStream.class))) {
                    doRead(callback, type, jp);
                }
                return;
            default:
                if (sc >= 400 && sc < 500) {
                    throw new ClientErrorException(response);
                }
                throw new ServerErrorException(response);
            }
        } finally {
            response.close();
        }
    }

    private <T> void doRead(
            Callback<T> callback,
            TypeReference<T> type,
            final JsonParser jp)
    throws IOException
    {
        expect(jp, jp.nextToken(), JsonToken.START_OBJECT);
        expect(jp, jp.nextToken(), JsonToken.FIELD_NAME);
        if (!"results".equals(jp.getCurrentName())) {
            throw new JsonParseException(jp, "expecting results field");
        }
        expect(jp, jp.nextToken(), JsonToken.START_ARRAY);
        // As noted in a well-hidden comment in the MappingIterator constructor,
        // readValuesAs requires the parser to be positioned after the START_ARRAY
        // token with an empty current token
        jp.clearCurrentToken();

        Iterator<T> iter = jp.readValuesAs(type);

        while (iter.hasNext()) {
            try {
                callback.call(iter.next());
            }
            catch (CallbackRefusedException e) {
                LOG.debug("callback refused execution, finishing.", e);
                return;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Callback interrupted", e);
            }
            catch (Exception e) {
                Throwables.propagateIfPossible(e, IOException.class);
                throw new IOException("Callback failure", e);
            }
        }
        if (jp.nextValue() != JsonToken.VALUE_TRUE || !"success".equals(jp.getCurrentName())) {
            throw new IOException("Streamed receive did not terminate normally; inspect server logs for cause.");
        }
    }

    private void expect(final JsonParser jp, final JsonToken token, final JsonToken expected) throws JsonParseException
    {
        if (!Objects.equal(token, expected)) {
            throw new JsonParseException(jp, String.format("Expected %s, found %s", expected, token));
        }
    }
}
