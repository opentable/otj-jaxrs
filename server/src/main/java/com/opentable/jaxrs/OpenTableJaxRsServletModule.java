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

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Scopes;
import com.google.inject.servlet.ServletModule;

import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import com.opentable.config.Config;
import com.opentable.httpserver.HttpServerHandlerBinder;
import com.opentable.jaxrs.exceptions.OpenTableJaxRsExceptionMapperModule;
import com.opentable.server.OTCorsFilter;

public class OpenTableJaxRsServletModule extends ServletModule
{
    private final List<String> paths;

    public OpenTableJaxRsServletModule(final Config config)
    {
        this(config, "/*");
    }

    public OpenTableJaxRsServletModule(Config config, String... paths) {
        this(config, Arrays.asList(paths));
    }

    public OpenTableJaxRsServletModule(Config config, List<String> paths) {
        Preconditions.checkNotNull(config, "null config");
        Preconditions.checkNotNull(paths, "null paths");
        Preconditions.checkArgument(paths.size() >= 1, "must serve at least one path");
        this.paths = ImmutableList.copyOf(paths);
    }

    @Override
    protected void configureServlets()
    {
        install (new JaxRsSharedModule());
        install (new OpenTableJaxRsExceptionMapperModule());

        JaxRsServerBinder.bindResponseFilter(binder()).to(JsonUtf8ResponseFilter.class);

        HttpServerHandlerBinder.bindServletContextListener(binder()).to(GuiceResteasyBootstrapServletContextListener.class);
        bind (GuiceResteasyBootstrapServletContextListener.class);

        bind (OTCorsFilter.class).asEagerSingleton();

        String first = paths.get(0);
        String[] rest = paths.subList(1, paths.size()).toArray(new String[paths.size()-1]);
        bind (HttpServlet30Dispatcher.class).in(Scopes.SINGLETON);
        serve(first, rest).with(HttpServlet30Dispatcher.class);
    }
}
