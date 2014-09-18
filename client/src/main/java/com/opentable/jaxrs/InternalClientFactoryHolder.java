package com.opentable.jaxrs;

import javax.ws.rs.client.ClientBuilder;

/**
 * Hides gory details of reflection from main API.
 *
 * Finds and remembers the Class for InternalClientFactory, uses it
 * to create new instances.
 */
class InternalClientFactoryHolder
{
    private static final InternalClientFactory FACTORY_IMPL = findFactory();

    static ClientBuilder newBuilder(JaxRsClientConfig config)
    {
        return FACTORY_IMPL.newBuilder(config);
    }

    private static InternalClientFactory findFactory() {
        try {
            final ClassLoader classLoader = InternalClientFactoryHolder.class.getClassLoader();
            final String implClass = "com.opentable.jaxrs.JaxRsClientFactoryImpl";
            return InternalClientFactory.class.cast(
                    Class.forName(implClass, true, classLoader).newInstance());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't find com.opentable.jaxrs.JaxRsClientBuilderImpl. " +
                    "did you include a jaxrs-clientbuilder-* jar on your classpath?", e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error instantiating factory class", e);
        }
    }
}
