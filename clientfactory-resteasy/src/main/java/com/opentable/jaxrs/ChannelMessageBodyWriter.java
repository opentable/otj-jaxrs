package com.opentable.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.strands.channels.Channel;

import com.opentable.jaxrs.json.JaxRsJsonStreamer;

@Provider
public class ChannelMessageBodyWriter implements MessageBodyWriter<Channel<?>> {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelMessageBodyWriter.class);
    private final ObjectMapper mapper;

    @Inject
    ChannelMessageBodyWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Channel.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Channel<?> t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Channel<?> t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
    throws IOException, WebApplicationException
    {
        LOG.trace("Begin streaming channel: {}", t);
        JaxRsJsonStreamer.wrappedResultsArrayOf(Object.class).build(mapper).execute(c -> {
            while (!t.isClosed()) {
                final Object resp = t.receive(30, TimeUnit.SECONDS);
                LOG.trace("channel {} received {}", t, resp);
                if (resp != null || !t.isClosed()) {
                    c.call(resp);
                }
            }
            LOG.trace("End streaming channel: {}", t);
        }).write(entityStream);
    }
}
