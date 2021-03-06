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

/**
 * A selection of {@link javax.ws.rs.client.Client} properties which
 * are known to work well in both Jersey and RESTEasy.
 */
public final class JaxRsClientProperties {
    public static final String ACTUAL_REQUEST = "ot.actual-request";
    public static final String FOLLOW_REDIRECTS = "jersey.config.client.followRedirects";

    private JaxRsClientProperties() { }
}
