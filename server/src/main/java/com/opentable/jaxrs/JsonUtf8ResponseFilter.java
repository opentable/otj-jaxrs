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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

/**
 * Enforces charset=utf8 on our json responses. Strictly spoken this is not necessary because application/json is inherently UTF-8 according to the
 * RFC, but it seems that there are client libraries out there that do not know that. So let's be sure.
 */
public class JsonUtf8ResponseFilter implements ContainerResponseFilter
{
    public static final MediaType APPLICATION_JSON_UTF8_TYPE = new MediaType("application", "json", Collections.singletonMap("charset", "utf-8"));
    public static final MediaType TEXT_JSON = new MediaType("text", "json");

    private static final List<Object> CONTENT_TYPE_HEADERS = Collections.<Object>singletonList(APPLICATION_JSON_UTF8_TYPE.toString());

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response)
    {
        if (MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()) || TEXT_JSON.isCompatible(response.getMediaType())) {
            response.getHeaders().put(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_HEADERS);
        }
    }
}
