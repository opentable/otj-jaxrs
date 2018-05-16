package com.opentable.jaxrs;

import java.security.KeyStore;
import java.security.cert.CRL;

public interface TlsProvider {
    CRL crl();
    void init(TlsUpdater updater);

    interface TlsUpdater {
        void install(KeyStore trustStore, KeyStore keyStore);
    }
}
