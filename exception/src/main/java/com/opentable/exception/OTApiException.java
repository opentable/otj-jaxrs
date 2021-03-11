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
package com.opentable.exception;

import java.util.Map;
import java.util.Objects;

import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.StatusType;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * API exception base class which has automatic transparency through
 * Jersey and HttpClient.  Attempts to remain mostly human-consumable.
 */
@Deprecated
public abstract class OTApiException extends ResponseProcessingException
{
    private static final long serialVersionUID = 1L;

    public static final String ERROR_TYPE = "errorType";
    public static final String ERROR_SUBTYPE = "errorSubtype";
    public static final String FIELD = "field";
    public static final String DETAIL = "detail";

    public static final MediaType MEDIA_TYPE = MediaType.valueOf("x-ot/error+json");

    private final Map<String, Object> fields;

    protected OTApiException(Map<String, ? extends Object> fields)
    {
        super(null, Objects.toString(fields.get(DETAIL)));
        checkNotBlank(fields, ERROR_TYPE);
        checkNotBlank(fields, ERROR_SUBTYPE);
        this.fields = ImmutableMap.copyOf(fields);
    }

    public abstract StatusType getStatus();

    public String getErrorType()
    {
        return getValue(ERROR_TYPE);
    }

    public String getErrorSubtype()
    {
        return getValue(ERROR_SUBTYPE);
    }

    public String getField()
    {
        return getValue(FIELD);
    }

    public String getDetail()
    {
        return getValue(DETAIL);
    }

    protected String getValue(String field)
    {
        return Objects.toString(fields.get(field), null);
    }

    @JsonValue
    public Map<String, Object> getJsonRepresentation()
    {
        return fields;
    }

    private static void checkNotBlank(Map<String, ? extends Object> fields, String fieldName)
    {
        final String field = Objects.toString(fields.get(fieldName), null);
        Preconditions.checkArgument(field != null && !field.trim().equals(""), "Field %s may not be missing", fieldName);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " [fields=" + fields + "]";
    }
}
