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
import static org.junit.Assert.fail;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kitei.testing.lessio.AllowNetworkAccess;

import com.opentable.config.Config;
import com.opentable.exception.ExceptionType;
import com.opentable.exception.OTApiException;
import com.opentable.exception.OTApiExceptionBinder;
import com.opentable.exception.OTApiExceptionModule;
import com.opentable.httpclient.HttpClient;
import com.opentable.httpclient.guice.HttpClientModule;
import com.opentable.httpclient.response.HttpResponseException;
import com.opentable.httpclient.response.Valid2xxContentConverter;
import com.opentable.jackson.OpenTableJacksonModule;
import com.opentable.jaxrs.OpenTableJaxRsServletModule;
import com.opentable.lifecycle.junit.LifecycleRunner;
import com.opentable.lifecycle.junit.LifecycleStatement;
import com.opentable.testing.IntegrationTestRule;
import com.opentable.testing.IntegrationTestRuleBuilder;
import com.opentable.testing.tweaked.TweakedModule;

@AllowNetworkAccess(endpoints= {"0.0.0.0:8080"})
@RunWith(LifecycleRunner.class)
public class TestExceptionMappingBinding
{
    private static final Config EMPTY_CONFIG = Config.getEmptyConfig();

    @Rule
    public LifecycleStatement lifecycle = LifecycleStatement.serviceDiscoveryLifecycle();

    @Rule
    public IntegrationTestRule rule = IntegrationTestRuleBuilder.defaultBuilder()
        .addService("boom", TweakedModule.forServiceModule(new AbstractModule() {
            @Override
            protected void configure()
            {
                install (new OpenTableJaxRsServletModule(EMPTY_CONFIG));
                install (new OpenTableJacksonModule());
                install (new OTApiExceptionModule("boom"));
                OTApiExceptionBinder.of(binder(), "boom").registerExceptionClass(BoomException.class);
                bind (BoomResource.class);
            }
        }))
        .build(this, new AbstractModule() {
            @Override
            protected void configure()
            {
                install (lifecycle.getLifecycleModule());
                install (new HttpClientModule("with"));
                install (new HttpClientModule("without"));

                install (new OTApiExceptionModule("with"));
                OTApiExceptionBinder.of(binder(), "with").registerExceptionClass(BoomException.class);
            }
        });

    @Inject
    @Named("with")
    HttpClient mappingClient;

    @Inject
    @Named("without")
    HttpClient regularClient;

    @Test(expected=BoomException.class)
    public void testWithMapping() throws Exception
    {
        mappingClient.get(UriBuilder.fromUri(rule.locateService("boom")).path("/boom").build(), Valid2xxContentConverter.DEFAULT_FAILING_RESPONSE_HANDLER).perform();
    }

    @Test
    public void testNoMapping() throws Exception
    {
        try {
            regularClient.get(UriBuilder.fromUri(rule.locateService("boom")).path("/boom").build(), Valid2xxContentConverter.DEFAULT_FAILING_RESPONSE_HANDLER).perform();
        } catch (HttpResponseException e) {
            assertEquals(BOOM_STATUS.getStatusCode(), e.getStatusCode());
            return;
        }
        fail();
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
