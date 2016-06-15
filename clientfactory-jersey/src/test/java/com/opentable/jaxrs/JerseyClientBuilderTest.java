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

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import javax.ws.rs.client.Client;

import org.junit.Test;

public class JerseyClientBuilderTest
{
    @Test
    public void socketTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        final JaxRsClientConfiguration conf = makeConfig();
        final Client client = factoryForConfig(conf).newClient("test", StandardFeatureGroup.PUBLIC);
        String result = client.getConfiguration().getProperty("jersey.config.client.readTimeout").toString();
        assertEquals("6600", result);
    }

    @Test
    public void connectTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        final JaxRsClientConfiguration conf = makeConfig();
        final Client client = factoryForConfig(conf).newClient("test", StandardFeatureGroup.PUBLIC);
        final String result = client.getConfiguration().getProperty("jersey.config.client.connectTimeout").toString();
        assertEquals("4400", result);
    }

    private static JaxRsClientFactory factoryForConfig(JaxRsClientConfiguration config) {
        return new JaxRsClientFactory(config);
    }

    private static JaxRsClientConfiguration makeConfig() {
        return new JaxRsClientConfiguration() {
            @Override
            public Duration socketTimeout() {
                return Duration.ofMillis(6600);
            }
            @Override
            public Duration connectTimeout() {
                return Duration.ofMillis(4400);
            }
        };
    }
}
