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

import java.util.WeakHashMap;

import org.springframework.context.ApplicationContext;

/**
 * Hides gory details of reflection from main API.
 *
 * Finds and remembers the Class for InternalClientFactory, uses it
 * to create new instances.
 */
final class InternalClientFactoryHolder {
    private static final InternalClientFactory FACTORY_IMPL = findFactory(null);
    private static final WeakHashMap<ApplicationContext, InternalClientFactory> factories = new WeakHashMap<>();

    private InternalClientFactoryHolder() { }

    static synchronized InternalClientFactory factory(ApplicationContext ctx) {
        if (ctx == null) {
            return FACTORY_IMPL;
        } else {
            return factories.computeIfAbsent(ctx, InternalClientFactoryHolder::findFactory);
        }
    }

    private static InternalClientFactory findFactory(ApplicationContext ctx) {
        try {
            final ClassLoader classLoader = InternalClientFactoryHolder.class.getClassLoader(); //NOPMD
            final String implClass = "com.opentable.jaxrs.JaxRsClientFactoryImpl";
            return InternalClientFactory.class.cast(
                    Class.forName(implClass, true, classLoader).getConstructor(ApplicationContext.class).newInstance(ctx));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't find com.opentable.jaxrs.JaxRsClientBuilderImpl. " +
                    "did you include a jaxrs-clientbuilder-* jar on your classpath?", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Error instantiating factory class", e);
        }
    }
}
