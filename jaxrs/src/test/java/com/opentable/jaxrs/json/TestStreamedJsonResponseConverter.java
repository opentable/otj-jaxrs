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
package com.opentable.jaxrs.json;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.easymock.EasyMock;
import org.junit.Test;

import com.opentable.callback.Callback;
import com.opentable.callback.CallbackCollector;
import com.opentable.callback.CallbackRefusedException;

public class TestStreamedJsonResponseConverter
{
    private static final TypeReference<Integer> INT_TYPE_REF = new TypeReference<Integer>() {};

    public static final String TEST_JSON = "{\"results\": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], \"success\":true}";
    public static final String EMPTY_JSON = "{\"results\": [], \"success\":true}";

    private final ObjectMapper mapper = new ObjectMapper();
    private final StreamedJsonResponseConverter streamer = new StreamedJsonResponseConverter(mapper);

    private static InputStream inputStream(String json)
    {
        return new ByteArrayInputStream(json.getBytes(Charsets.UTF_8));
    }
    private static Response response(int status, InputStream entity)
    {
        Response response = EasyMock.createMock(Response.class);
        EasyMock.expect(response.getStatus()).andReturn(status);
        EasyMock.expect(response.readEntity(InputStream.class)).andReturn(entity);
        EasyMock.replay(response);
        return response;
    }

    @Test
    public void testSuccess() throws Exception
    {
        CallbackCollector<Integer> callback = new CallbackCollector<>();
        streamer.read(response(200, inputStream(TEST_JSON)), callback, INT_TYPE_REF);

        assertEquals(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), callback.getItems());
    }

    @Test
    public void testRefuse() throws Exception
    {
        final List<Integer> items = Lists.newArrayList();
        Callback<Integer> callback = new Callback<Integer>() {
            @Override
            public void call(Integer item) throws Exception
            {
                if (item >= 5) {
                    throw new CallbackRefusedException();
                }

                items.add(item);
            }
        };
        streamer.read(response(200, inputStream(TEST_JSON)), callback, INT_TYPE_REF);

        assertEquals(ImmutableList.of(1, 2, 3, 4), items);
    }

    @Test
    public void testEmpty() throws Exception
    {
        CallbackCollector<Integer> callback = new CallbackCollector<>();
        streamer.read(response(200, inputStream(EMPTY_JSON)), callback, INT_TYPE_REF);
    }
}
