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

import net.jpountz.lz4.LZ4BlockInputStream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.io.InputStream;

@Provider
@Priority(Priorities.ENTITY_CODER)
public class LZ4DecodingInterceptor implements ReaderInterceptor
{
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException
    {
        Object encoding = context.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);

        if (encoding != null && encoding.toString().equalsIgnoreCase("lz4"))
        {
            InputStream old = context.getInputStream();
            LZ4BlockInputStream is = new LZ4BlockInputStream(old);
            context.setInputStream(is);
            try
            {
                return context.proceed();
            }
            finally
            {
                context.setInputStream(old);
            }
        }
        else
        {
            return context.proceed();
        }
    }
}
