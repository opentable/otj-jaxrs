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
package com.opentable.jaxrs.json;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.callback.Callback;

/**
 * A configurable JAX-RS Json output streamer.  Allows customization of the header, footer, and how
 * each element is written.
 */
public class JaxRsJsonStreamer<T>
{
    private static final Logger LOG = LoggerFactory.getLogger(JaxRsJsonStreamer.class);

    private final ObjectMapper mapper;
    private final Callback<JsonGenerator> header;
    private final Callback<JsonGenerator> footer;
    private final JsonEmitter<T> emitter;

    public interface JsonEmitter<T>
    {
        void emit(JsonGenerator g, T element) throws IOException;
    }

    public static class Builder<T>
    {
        private Callback<JsonGenerator> header;
        private Callback<JsonGenerator> footer;

        @SuppressWarnings("unchecked")
        private JsonEmitter<T> emitter = (JsonEmitter<T>) StandardStreamCallbacks.DEFAULT_EMITTER;

        private final TypeReference<T> type;

        public Builder(TypeReference<T> type)
        {
            this.type = type;
        }

        public Builder<T> withHeader(Callback<JsonGenerator> withHeader)
        {
            this.header = withHeader;
            return this;
        }

        public Builder<T> withFooter(Callback<JsonGenerator> withFooter)
        {
            this.footer = withFooter;
            return this;
        }

        public Builder<T> withEmitter(JsonEmitter<T> withEmitter)
        {
            this.emitter = withEmitter;
            return this;
        }

        public JaxRsJsonStreamer<T> build(ObjectMapper mapper)
        {
            return new JaxRsJsonStreamer<T>(mapper, type, emitter, header, footer);
        }
    }

    public static <T> Builder<T> builder(TypeReference<T> type)
    {
        return new Builder<T>(type);
    }

    /**
     * Convenience method to create responses in the "standard wrapped results" style.
     * <pre>
     * {
     *     "results": [
     *         item,
     *         item,
     *         ...
     *     ],
     *     "success": true
     * }
     * </pre>
     * @param type the generic type
     * @param <T> generic type
     * @return Builder
     */
    public static <T> Builder<T> wrappedResultsArrayOf(TypeReference<T> type)
    {
        return builder(type).withHeader(StandardStreamCallbacks.RESULTS_HEADER).withFooter(StandardStreamCallbacks.RESULTS_FOOTER);
    }

    /**
     * Convenience method to create responses in a bare array.
     * <pre>
     * [
     *     item,
     *     item,
     *     ...
     * ]
     * </pre>
     * @param type the generic type
     * @param <T> generic type
     * @return Builder
     */
    public static <T> Builder<T> arrayOf(TypeReference<T> type)
    {
        return builder(type).withHeader(StandardStreamCallbacks.ARRAY_HEADER).withFooter(StandardStreamCallbacks.ARRAY_FOOTER);
    }

    JaxRsJsonStreamer(ObjectMapper mapper, TypeReference<T> type, JsonEmitter<T> emitter, Callback<JsonGenerator> header, Callback<JsonGenerator> footer)
    {
        this.emitter = checkNotNull(emitter, "null emitter");
        this.mapper = checkNotNull(mapper, "null mapper");
        checkNotNull(type, "null type");
        this.header = checkNotNull(header, "no header writer set");
        this.footer = checkNotNull(footer, "no footer writer set");
    }

    public StreamingOutput execute(Callback<Callback<T>> callback)
    {
        return new JsonStreamingOutput(callback);
    }

    private class JsonStreamingOutput implements StreamingOutput
    {
        private final Callback<Callback<T>> callback;

        JsonStreamingOutput(Callback<Callback<T>> callback)
        {
            this.callback = callback;
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException
        {
            final StopWatch sw = new StopWatch();
            sw.start();

            LOG.trace("Start streaming {}", JaxRsJsonStreamer.this);
            final AtomicLong count = new AtomicLong();
            boolean success = false;

            try (JsonGenerator jg = mapper.getFactory().createGenerator(output)) {

                header.call(jg);

                callback.call(new Callback<T>() {
                    @Override
                    public void call(T item) throws Exception
                    {
                        emitter.emit(jg, item);
                    }
                });

                footer.call(jg);

                success = true;
            } catch (final Exception t) {
                Throwables.propagateIfPossible(t, WebApplicationException.class, IOException.class);
                throw new RuntimeException(t);
            } finally {
                if (success) {
                    LOG.trace("Succeeded streaming {} results in {}ms for {}", count.get(), sw.getTime(), JaxRsJsonStreamer.this);
                } else {
                    LOG.debug("Failed streaming after {} results in {}ms for {}", count.get(), sw.getTime(), JaxRsJsonStreamer.this);
                }
            }
        }
    }
}
