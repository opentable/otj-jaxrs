package com.opentable.jaxrs;

import javax.ws.rs.core.Response;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.io.ChunkedInputStream;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.specimpl.BuiltResponse;

public final class ResteasyUtil {
    private ResteasyUtil() { BuiltResponse.class.cast(null); } // fool dependency analyzer
    /**
     * Given a {@code Response} object, find the associated http request and abort it.
     * Mostly useful to cleanly close never ending streaming responses -- if you call
     * {@link ChunkedInputStream#close()} without first aborting it hangs forever.
     *
     * @see https://issues.jboss.org/browse/RESTEASY-1478
     * @see http://mail-archives.apache.org/mod_mbox/hc-httpclient-users/201608.mbox/%3CD93DAC13-ABE5-4E3C-9429-82D3C94838C9%40gmail.com%3E
     */
    public static void abortResponse(Response r) {
        final ClientResponse clientResponse = (ClientResponse)r; //NOPMD
        ((HttpRequestBase)(clientResponse.getProperties().get(JaxRsClientProperties.ACTUAL_REQUEST))).abort();
    }
}
