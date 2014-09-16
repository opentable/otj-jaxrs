package com.opentable.jaxrs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.opentable.callback.Callback;
import com.opentable.callback.CallbackRefusedException;
import com.opentable.logging.Log;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Singleton
public class StreamedJsonResponseConverter
{
    private static final Log LOG = Log.findLog();

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
        final int sc = response.getStatus();
        switch(sc)
        {
        case 201:
        case 204:
            LOG.debug("Return code is %d, finishing.", response.getStatus());
            return;
        case 200:
            try (final JsonParser jp = mapper.getFactory().createParser(response.readEntity(InputStream.class))) {
                doRead(callback, type, jp);
            }
            return;
        default:
            if (sc >= 400 && sc < 500) {
                throw new ClientErrorException(response);
            }
            throw new ServerErrorException(response);
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
            throw new JsonParseException("expecting results field", jp.getCurrentLocation());
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
                LOG.debug(e, "callback refused execution, finishing.");
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
        if (jp.nextValue() != JsonToken.VALUE_TRUE || !jp.getCurrentName().equals("success")) {
            throw new IOException("Streamed receive did not terminate normally; inspect server logs for cause.");
        }
    }

    private void expect(final JsonParser jp, final JsonToken token, final JsonToken expected) throws JsonParseException
    {
        if (!Objects.equal(token, expected)) {
            throw new JsonParseException(String.format("Expected %s, found %s", expected, token), jp.getCurrentLocation());
        }
    }
}
