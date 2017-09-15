package com.opentable.jaxrs.util;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public final class HttpHeadersUtils {
    private HttpHeadersUtils() {
    }

    /**
     * Implementation of {@link HttpHeaders} based on {@link ContainerRequestContext}.
     */
    public static HttpHeaders from(final ContainerRequestContext ctx) {
        return new HttpHeaders() {
            @Override
            public List<String> getRequestHeader(String name) {
                return getRequestHeaders().get(name);
            }

            @Override
            public String getHeaderString(String name) {
                return ctx.getHeaderString(name);
            }

            @Override
            public MultivaluedMap<String, String> getRequestHeaders() {
                return ctx.getHeaders();
            }

            @Override
            public List<MediaType> getAcceptableMediaTypes() {
                return ctx.getAcceptableMediaTypes();
            }

            @Override
            public List<Locale> getAcceptableLanguages() {
                return ctx.getAcceptableLanguages();
            }

            @Override
            public MediaType getMediaType() {
                return ctx.getMediaType();
            }

            @Override
            public Locale getLanguage() {
                return ctx.getLanguage();
            }

            @Override
            public Map<String, Cookie> getCookies() {
                return ctx.getCookies();
            }

            @Override
            public Date getDate() {
                return ctx.getDate();
            }

            @Override
            public int getLength() {
                return ctx.getLength();
            }
        };
    }
}
