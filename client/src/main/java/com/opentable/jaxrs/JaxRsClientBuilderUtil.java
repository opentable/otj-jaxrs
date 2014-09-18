package com.opentable.jaxrs;

/**
 * Hides gory details of reflection from main builder API.
 *
 * Finds and remembers the Class for JaxRsClientBuilderImpl, uses it
 * to create new instances.
 */
class JaxRsClientBuilderUtil
{
    private static Class<JaxRsClientBuilder> builderClass = findBuilderClass();

    static JaxRsClientBuilder newInstance()
    {
        try {
            return builderClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Can't find com.opentable.jaxrs.clientfactory.JaxRsClientBuilderImpl. " +
                    "did you include a jaxrs-clientbuilder-* jar on your classpath?", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<JaxRsClientBuilder> findBuilderClass() {
        try {
            final ClassLoader classLoader = JaxRsClientBuilderUtil.class.getClassLoader();
            final String implClass = "com.opentable.jaxrs.clientfactory.JaxRsClientBuilderImpl";
            return (Class<JaxRsClientBuilder>) Class.forName(implClass, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Looks like no implementation of ot-jaxrs-clientbuilder was supplied", e);
        }
    }
}
