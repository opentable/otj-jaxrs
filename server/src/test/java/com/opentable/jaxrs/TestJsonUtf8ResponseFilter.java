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

import org.easymock.EasyMock;
import org.jboss.resteasy.core.interception.ContainerResponseContextImpl;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class TestJsonUtf8ResponseFilter
{
    ContainerRequestContext req;

    @Before
    public void createRequest()
    {
        req = EasyMock.createNiceMock(ContainerRequestContext.class);
        EasyMock.replay(req);
    }

    private ContainerResponseContext response(ResponseBuilder built)
    {
        return new ContainerResponseContextImpl(null, null, (BuiltResponse)built.build());
    }

    @Test
    public void testSimple()
    {
        final JsonUtf8ResponseFilter filter = new JsonUtf8ResponseFilter();

        final ContainerResponseContext c = response(Response.ok().type(MediaType.APPLICATION_JSON_TYPE));

        filter.filter(null, c);

        Assert.assertEquals(JsonUtf8ResponseFilter.APPLICATION_JSON_UTF8_TYPE, c.getMediaType());
    }

    @Test
    public void testUnchanged()
    {
        final JsonUtf8ResponseFilter filter = new JsonUtf8ResponseFilter();

        final ContainerResponseContext c = response(Response.ok().type(MediaType.APPLICATION_XML_TYPE));

        filter.filter(null, c);

        Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, c.getMediaType());
    }

    @Test
    public void testAlready()
    {
        final JsonUtf8ResponseFilter filter = new JsonUtf8ResponseFilter();

        final ContainerResponseContext c = response(Response.ok().type(JsonUtf8ResponseFilter.APPLICATION_JSON_UTF8_TYPE));

        filter.filter(null, c);

        Assert.assertEquals(JsonUtf8ResponseFilter.APPLICATION_JSON_UTF8_TYPE, c.getMediaType());
    }

    @Test
    public void testUnset()
    {
        final JsonUtf8ResponseFilter filter = new JsonUtf8ResponseFilter();

        final ContainerResponseContext c = response(Response.ok());

        filter.filter(null, c);

        Assert.assertNull(c.getMediaType());
    }

    @Test
    public void testHowAboutAWildcard()
    {
        final JsonUtf8ResponseFilter filter = new JsonUtf8ResponseFilter();

        final ContainerResponseContext c = response(Response.ok().type(new MediaType("*", "json")));

        filter.filter(null, c);

        Assert.assertEquals(JsonUtf8ResponseFilter.APPLICATION_JSON_UTF8_TYPE, c.getMediaType());
    }

    @Test
    public void testHowAboutABadMediatype()
    {
        final JsonUtf8ResponseFilter filter = new JsonUtf8ResponseFilter();

        final ContainerResponseContext c = response(Response.ok().type(new MediaType("text", "json")));

        filter.filter(null, c);

        Assert.assertEquals(JsonUtf8ResponseFilter.APPLICATION_JSON_UTF8_TYPE, c.getMediaType());
    }

    @Test
    public void testDontTouchTheText()
    {
        final JsonUtf8ResponseFilter filter = new JsonUtf8ResponseFilter();

        final ContainerResponseContext c = response(Response.ok().type(MediaType.TEXT_PLAIN_TYPE));

        filter.filter(null, c);

        Assert.assertEquals(MediaType.TEXT_PLAIN_TYPE, c.getMediaType());
    }
}
