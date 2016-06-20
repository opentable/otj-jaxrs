package com.opentable.jaxrs;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;

import org.junit.Test;

public class DataUriTest {
    static final String TEST = "testtest";
    JaxRsClientFactory factory = new JaxRsClientFactory(t -> JaxRsClientConfig.DEFAULT);

    @Test
    public void testDataUri() {
        factory.addFeatureToAllClients(new DataUriFeature());
        Client c = factory.newClient("test", StandardFeatureGroup.PUBLIC);
        assertEquals(TEST, c.target("data:" + TEST).request().get(String.class));
    }
}
