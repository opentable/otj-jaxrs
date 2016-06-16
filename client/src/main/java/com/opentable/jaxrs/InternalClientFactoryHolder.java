/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    static ClientBuilder newBuilder(String clientName, JaxRsClientConfig config)
    {
        return FACTORY_IMPL.newBuilder(clientName, config);
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
