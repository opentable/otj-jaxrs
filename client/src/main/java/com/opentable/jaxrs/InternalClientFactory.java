package com.opentable.jaxrs;

import javax.ws.rs.client.ClientBuilder;

/**
 * SPI for creating {@link ClientBuilder} instances.
 */
interface InternalClientFactory {
    ClientBuilder newBuilder(JaxRsClientConfig config);
}
