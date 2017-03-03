OpenTable JAX-RS Component
==========================

[![Build Status](https://travis-ci.org/opentable/otj-jaxrs.svg)](https://travis-ci.org/opentable/otj-jaxrs)

Component Charter
-----------------

* Provides bindings from a JAX-RS Client provider into the Spring, configuration, and `otj-server` ecosystem.
  - Can swap out Jersey for RESTEasy
  - Register Features with Spring DI environment
* Brings in `otj-jackson` support for directly reading and writing JSON.

Component Level
---------------

Configuration
--------------
The JAX-RS client configuration is managed through your application properties. 


Options are configured using the provided client name and the corresponding jaxrs configuration:  
    `jaxrs.client.${clientName}.option=value`


For the client name "availability" the corresponding connection pool configuration option will be:  
    `jaxrs.client.availability.connectionPool=50`


For configuration options that take time, use the ISO 8601 Durations format (https://en.wikipedia.org/wiki/ISO_8601).  

Example: Configure the connection timeout to 5s  
    `jaxrs.client.availability.connectTimeout=PT5s`


For values smaller than seconds you may use decimals per the ISO 8601 format:  
>"The smallest value used may also have a decimal fraction, as in "P0.5Y" to indicate half a year. This decimal fraction may be specified with either a comma or a full stop, as in "P0,5Y" or "P0.5Y"."


To configure a connection timeout of 150ms:  
    `jaxrs.client.availability.connectTimeout=PT0.150s`


For a list of configurable options see [client/src/main/java/com/opentable/jaxrs/JaxRsClientConfig.java](client/src/main/java/com/opentable/jaxrs/JaxRsClientConfig.java)

----
Copyright (C) 2014 OpenTable, Inc.
