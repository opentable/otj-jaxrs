otj-jaxrs changelog
===================

2.9.10
------
* As part of security coverage, we now scrub the user-agent field. You may also now configure the field yourself.

2.9.9
-----
REQUIRED FOR JAVA 11
* Current version of Java 11 has issues with TLS 1.3 (which is new in Java 11). This introduces
a switch, that we can toggle TLS 1.3 on/off. It is currently as of publication toggled in
otpl-common-config and is set to disable.

2.9.8
-----
* Small bug in CalculateThreads

2.9.7
-----
* Auto tuning is on by default in response to the incident with 16 cores.

2.9.6
-----
* Clarify supported engines in JAXRSClientConfig
* Add otj-resteasy-apache for folks that might need it (please see us if you do)

2.9.5
-----
* Rebuild DAG for SBT 2.1.1
2.9.4
-----
* We now support a simple HTTP proxy with the JAXRS Jetty Client

(See ProxyHost, ProxyPort in JAXRSClientConfig)

2.9.3
-----
A deadlock condition is possible in JaxRS Client. It should only occur on machines > 8 cores.

We added getExecutorThreads() to the config to deal with it. Setting to -1 should
autocalculate a good value

2.9.2
-----
* Update to Jetty 9.4.12.0830

2.9.1
-----
* Update to Resteasy Beta 5 (from OT-3 which was a melange of beta3++)

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
