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
package com.nesscomputing.jersey.types;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Simple Jersey date parameter class.  Accepts either milliseconds since epoch UTC or ISO formatted dates.
 * Will convert everything into UTC regardless of input timezone.
 */
public class DateParam
{
    private static final ZoneId UTC_ID = ZoneId.of("UTC");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+");

    private final ZonedDateTime dateTime;

    DateParam(ZonedDateTime dateTime)
    {
        this.dateTime = checkNotNull(dateTime, "null datetime").withZoneSameInstant(UTC_ID);
    }

    public static DateParam valueOf(ZonedDateTime dateTime)
    {
        return new DateParam(dateTime);
    }

    public static DateParam valueOf(String string)
    {
        if (string == null) {
            return null;
        }

        if (NUMBER_PATTERN.matcher(string).matches()) {
            return new DateParam(ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(string)), UTC_ID));
        } else {
            return new DateParam(ZonedDateTime.parse(string));
        }
    }

    /**
     * @return a DateTime if the parameter was provided, or null otherwise.
     */
    // This method is static so that you can handle optional parameters as null instances.
    public static ZonedDateTime getDateTime(DateParam param)
    {
        return param == null ? null : param.dateTime;
    }

    @Override
    public String toString()
    {
        return Objects.toString(dateTime);
    }
}
