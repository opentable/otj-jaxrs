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
package com.opentable.jaxrs.types;

import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.google.inject.AbstractModule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.opentable.config.Config;
import com.opentable.jaxrs.JaxRsClientModule;
import com.opentable.jaxrs.ServerBaseModule;
import com.opentable.jaxrs.StandardFeatureGroup;
import com.opentable.jaxrs.json.OTJacksonJsonProvider;
import com.opentable.lifecycle.junit.LifecycleRule;
import com.opentable.lifecycle.junit.LifecycleRunner;
import com.opentable.lifecycle.junit.LifecycleStatement;
import com.opentable.testing.IntegrationTestRule;
import com.opentable.testing.IntegrationTestRuleBuilder;
import com.opentable.testing.tweaked.TweakedModule;

@RunWith(LifecycleRunner.class)
public class DateParamTest
{
    private static final String DATE_TEST_SERVICE_NAME = "datetest";

    @LifecycleRule
    public final LifecycleStatement lifecycleRule = LifecycleStatement.serviceDiscoveryLifecycle();

    private UriBuilder uriBuilder;

    @Rule
    public IntegrationTestRule test = IntegrationTestRuleBuilder.defaultBuilder()
        .addService(DATE_TEST_SERVICE_NAME, TweakedModule.forServiceModule(DateToLongModule.class))
        .addTestCaseModules(lifecycleRule.getLifecycleModule(), new AbstractModule() {
            @Override
            protected void configure()
            {
                bind (Clock.class).toInstance(Clock.systemUTC());
                install (new JaxRsClientModule("test", StandardFeatureGroup.PLATFORM_INTERNAL));
                bind (OTJacksonJsonProvider.class);
            }
        })
        .build(this);

    @Inject
    @Named("test")
    private Client httpClient;

    @Before
    public void setUp()
    {
        uriBuilder = UriBuilder.fromUri(test.locateService(DATE_TEST_SERVICE_NAME)).path("/date");
    }

    @Test
    public void testDateLong() throws Exception
    {
        Instant when = Instant.ofEpochMilli(1000);
        assertEquals(when.toEpochMilli(),
                Long.parseLong(httpClient.target(
                        uriBuilder.queryParam("date", when.toEpochMilli()).build())
                        .request().get(String.class)));
    }

    @Test
    public void testDateString() throws Exception
    {
        Instant when = Instant.ofEpochMilli(1000);
        assertEquals(when.toEpochMilli(),
                Long.parseLong(httpClient.target(
                        uriBuilder.queryParam("date", DateTimeFormatter.ISO_DATE_TIME.format(when.atZone(ZoneId.of("UTC")))).build())
                        .request().get(String.class)));
    }

    @Test
    public void testDateTZString() throws Exception
    {
        Instant when = Instant.ofEpochMilli(1000);
        assertEquals(when.toEpochMilli(),
                Long.parseLong(httpClient.target(
                        uriBuilder.queryParam("date", when.atZone(ZoneId.of("America/Los_Angeles")).toString()).build())
                        .request().get(String.class)));
    }

    @Test
    public void testNull() throws Exception
    {
        assertEquals("asdf", httpClient.target(
                        uriBuilder.build()).request().get(String.class));
    }

    @Test
    public void testNegativeDate() throws Exception
    {
        Instant when = Instant.ofEpochMilli(-1000);
        assertEquals(when.toEpochMilli(),
                Long.parseLong(httpClient.target(
                        uriBuilder.queryParam("date", when.toEpochMilli()).build())
                        .request().get(String.class)));
    }

    public static class DateToLongModule extends AbstractModule {
        private final Config config;

        public DateToLongModule(Config config)
        {
            this.config = config;
        }

        @Override
        protected void configure() {
            bind (Clock.class).toInstance(Clock.systemUTC());
            install (new ServerBaseModule(config));
            bind (DateToLongResource.class);
        }
    }

    @Path("/date")
    @Produces(MediaType.TEXT_PLAIN)
    public static class DateToLongResource
    {
        @GET
        public String dateToLong(@QueryParam("date") DateParam dateTime)
        {
            if (DateParam.getDateTime(dateTime) == null) {
                return "asdf";
            }
            return Long.toString(DateParam.getDateTime(dateTime).toInstant().toEpochMilli());
        }
    }
}
