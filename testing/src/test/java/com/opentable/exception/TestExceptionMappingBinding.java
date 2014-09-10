/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
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
package com.opentable.exception;

import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.opentable.config.Config;
import com.opentable.jaxrs.JaxRsClientModule;
import com.opentable.jaxrs.ServerBaseModule;
import com.opentable.jaxrs.StandardFeatureGroup;
import com.opentable.jaxrs.json.OTJacksonJsonProvider;
import com.opentable.lifecycle.junit.LifecycleRunner;
import com.opentable.lifecycle.junit.LifecycleStatement;
import com.opentable.testing.IntegrationTestRule;
import com.opentable.testing.IntegrationTestRuleBuilder;
import com.opentable.testing.tweaked.TweakedModule;

@RunWith(LifecycleRunner.class)
public class TestExceptionMappingBinding
{
    @Rule
    public LifecycleStatement lifecycle = LifecycleStatement.serviceDiscoveryLifecycle();

    @Rule
    public IntegrationTestRule rule = IntegrationTestRuleBuilder.defaultBuilder()
        .addService("boom", TweakedModule.forServiceModule(new AbstractModule() {
            @Override
            protected void configure()
            {
                bind (Clock.class).toInstance(Clock.systemUTC());
                install (new ServerBaseModule(Config.getEmptyConfig()));
                install (new OTApiExceptionModule());
                OTApiExceptionBinder.of(binder()).registerExceptionClass(BoomException.class);
                bind (BoomResource.class);
            }
        }))
        .build(this, new AbstractModule() {
            @Override
            protected void configure()
            {
                install (lifecycle.getLifecycleModule());
                bind (OTJacksonJsonProvider.class);

                install (new JaxRsClientModule("mapping", StandardFeatureGroup.PLATFORM_INTERNAL));

                install (new OTApiExceptionModule());
                OTApiExceptionBinder.of(binder()).registerExceptionClass(BoomException.class);
            }
        });

    @Inject
    @Named("mapping")
    Client mappingClient;

    Client regularClient = ClientBuilder.newClient();

    @Test(expected=BoomException.class)
    public void testWithMapping() throws Exception
    {
        mappingClient.target(UriBuilder.fromUri(rule.locateService("boom")).path("/boom")).request().get();
    }

    @Test
    public void testNoMapping() throws Exception
    {
        Response response = regularClient.target(UriBuilder.fromUri(rule.locateService("boom")).path("/boom")).request().get();
        assertEquals(BOOM_STATUS.getStatusCode(), response.getStatus());
    }

    /** A status unlikely to be accidentally returned (i.e. not 500) */
    public static final StatusType BOOM_STATUS = Status.SERVICE_UNAVAILABLE;

    @Path("/boom")
    public static class BoomResource
    {
        @GET
        public String boom()
        {
            throw new BoomException();
        }
    }

    @ExceptionType("Boom")
    public static class BoomException extends OTApiException
    {
        BoomException()
        {
            this (ImmutableMap.of(
                    ERROR_TYPE, "Boom",
                    ERROR_SUBTYPE, "blam",
                    DETAIL, "boom"
                ));
        }

        protected BoomException(Map<String, ? extends Object> fields)
        {
            super(fields);
        }

        private static final long serialVersionUID = 1L;

        @Override
        public StatusType getStatus()
        {
            return BOOM_STATUS;
        }
    }
}
