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
package com.opentable.jaxrs.exceptions;

import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.MDC;

/**
 * A generic base class to map an exception to a Ness response. This must be implemented by a concrete
 * class because the Jersey IoC container requires concrete classes and could not deal with an inner class that
 * implements this. An example would be
 *
 * <pre>   @Provider
   public static final class IllegalArgumentExceptionMapper extends NessJerseyExceptionMapper&lt;IllegalArgumentException&gt;
   {
       public IllegalArgumentExceptionMapper()
       {
           super(Status.BAD_REQUEST, IllegalArgumentException.class);
       }
   }
   </pre>
 */
public abstract class OpenTableJaxRsExceptionMapper<U extends Throwable> implements ExceptionMapper<U>
{
    private final Status statusCode;

    protected OpenTableJaxRsExceptionMapper(final Status statusCode)
    {
        this.statusCode = statusCode;
    }

    @Override
    public Response toResponse(final U exception)
    {
        final Map<String, String> response = ImmutableMap.of("code", statusCode.toString(),
                                                             "trace", Objects.firstNonNull(MDC.get("track"), ""),
                                                             "message", ObjectUtils.firstNonNull(exception.getMessage(), "(no message)"));

        return Response.status(statusCode)
        .entity(response)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
    }
}
