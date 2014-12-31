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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

import org.junit.Test;
import org.skife.config.TimeSpan;

import com.opentable.config.Config;

public class JerseyClientBuilderTest
{
    @Test
    public void socketTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        final JaxRsClientConfig conf = makeConfig();
        final Client client = factoryForConfig(conf).newClient("test", StandardFeatureGroup.PUBLIC);
        String result = client.getConfiguration().getProperty("jersey.config.client.readTimeout").toString();
        assertEquals("6600", result);
    }

    @Test
    public void connectTimeoutPropagates() throws NoSuchFieldException, IllegalAccessException
    {
        final JaxRsClientConfig conf = makeConfig();
        final Client client = factoryForConfig(conf).newClient("test", StandardFeatureGroup.PUBLIC);
        final String result = client.getConfiguration().getProperty("jersey.config.client.connectTimeout").toString();
        assertEquals("4400", result);
    }

    private static JaxRsClientFactory factoryForConfig(JaxRsClientConfig config) {
        return new JaxRsClientFactory(Config.getEmptyConfig()) {
            @Override
            protected JaxRsClientConfig configForClient(String clientName) {
                return config;
            }
        };
    }

    private static JaxRsClientConfig makeConfig() {
        final JaxRsClientConfig conf = mock(JaxRsClientConfig.class);
        when(conf.socketTimeout()).thenReturn(new TimeSpan(6600, TimeUnit.MILLISECONDS));
        when(conf.connectTimeout()).thenReturn(new TimeSpan(4400, TimeUnit.MILLISECONDS));
        return conf;
    }
}
