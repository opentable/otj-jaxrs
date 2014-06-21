/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
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

import static com.opentable.exception.OTApiException.DETAIL;
import static com.opentable.exception.OTApiException.ERROR_SUBTYPE;
import static com.opentable.exception.OTApiException.ERROR_TYPE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class TestOTApiExceptionMapping
{

    @Inject
    ExceptionFilter filter;

    @Before
    public void setUp()
    {
        Guice.createInjector(new OTApiExceptionModule(), new AbstractModule() {
            @Override
            protected void configure()
            {
                OTApiExceptionBinder.of(binder()).registerExceptionClass(TestingException.class);
            }
        }).injectMembers(this);
    }

    @ExceptionType("foo")
    @ExceptionSubtype("baz")
    static class TestingException extends OTApiException
    {
        protected TestingException(Map<String, ? extends Object> fields)
        {
            super(fields);
        }

        private static final long serialVersionUID = 1L;

        @Override
        public Status getStatus()
        {
            return Status.INTERNAL_SERVER_ERROR;
        }
    }

    @Test(expected=UnknownOTApiException.class)
    public void testUnknownException() throws Exception
    {
        final Map<String, ?> error = ImmutableMap.of("causes", ImmutableList.of(ImmutableMap.of(
                ERROR_TYPE, "foo",
                ERROR_SUBTYPE, "bar",
                DETAIL, "bogus!"
            )));

        final InputStream data = new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(error));

        final ClientResponseContext response = EasyMock.createMock(ClientResponseContext.class);

        EasyMock.expect(response.getMediaType()).andReturn(OTApiException.MEDIA_TYPE);
        EasyMock.expect(response.getEntityStream()).andReturn(data);
        EasyMock.replay(response);

        filter.filter(null, response);
    }

    @Test(expected=TestingException.class)
    public void testKnownException() throws Exception
    {
        final Map<String, ?> error = ImmutableMap.of("causes", ImmutableList.of(ImmutableMap.of(
                ERROR_TYPE, "foo",
                ERROR_SUBTYPE, "baz",
                DETAIL, "bogus!"
            )));

        final InputStream data = new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(error));

        final ClientResponseContext response = EasyMock.createMock(ClientResponseContext.class);

        EasyMock.expect(response.getMediaType()).andReturn(OTApiException.MEDIA_TYPE);
        EasyMock.expect(response.getEntityStream()).andReturn(data);
        EasyMock.replay(response);

        filter.filter(null, response);
    }
}
