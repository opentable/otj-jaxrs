otj-jaxrs changelog
===================

2.9.0
-----

* first pass at jetty tls integration

2.8.3
-----

* fix leaking Client thread pools

2.8.2
-----
* Spring Boot 2/5.0.4

2.8.1
-----

* fix Jetty HttpEngine SSL support, client builder properties

2.8.0
-----

* use Jetty client HttpEngine for real async support

2.7.0
-----

* made referrer logic more robust to different spring configurations

2.6.0
-----

* added automatic referrer-related header setting for clients

2.5.0
-----

* added HttpHeadersUtils

2.4.2
-----

* findbugs-annotations change

2.4.1
-----

* MonitoredPoolingHttpClientConnectionManager: log call sites for open connections at TRACE level

2.4.0
-----

* jaxrs-clientfactory-resteasy: publicly expose Apache HttpClientBuilder for use by Twilio library
