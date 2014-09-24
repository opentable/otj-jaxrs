package com.opentable.jaxrs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opentable.callback.Callback;
import com.opentable.callback.CallbackCollector;
import com.opentable.callback.CallbackRefusedException;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

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
    private static Response response(Status status, InputStream entity)
    {
        Response response = EasyMock.createMock(Response.class);
        EasyMock.expect(response.getStatus()).andReturn(status.getStatusCode()).anyTimes();
        EasyMock.expect(response.getStatusInfo()).andReturn(status).anyTimes();
        EasyMock.expect(response.readEntity(InputStream.class)).andReturn(entity);
        response.close();
        EasyMock.expectLastCall();
        EasyMock.replay(response);
        return response;
    }

    @Test
    public void testSuccess() throws Exception
    {
        CallbackCollector<Integer> callback = new CallbackCollector<>();
        streamer.read(response(Status.OK, inputStream(TEST_JSON)), callback, INT_TYPE_REF);

        Assert.assertEquals(ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), callback.getItems());
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
        streamer.read(response(Status.OK, inputStream(TEST_JSON)), callback, INT_TYPE_REF);

        Assert.assertEquals(ImmutableList.of(1, 2, 3, 4), items);
    }

    @Test
    public void testEmpty() throws Exception
    {
        CallbackCollector<Integer> callback = new CallbackCollector<>();
        streamer.read(response(Status.OK, inputStream(EMPTY_JSON)), callback, INT_TYPE_REF);
    }
}
