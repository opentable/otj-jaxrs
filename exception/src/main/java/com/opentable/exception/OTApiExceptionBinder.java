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

import com.google.inject.Binder;
import com.google.inject.multibindings.MapBinder;

/**
 * Register OtApiException subclasses so they may be correctly mapped to and from HTTP responses.
 */
public final class OTApiExceptionBinder
{
    private final MapBinder<String, ExceptionReviver> mapBinder;

    private OTApiExceptionBinder(Binder binder)
    {
        mapBinder = MapBinder.newMapBinder(binder, String.class, ExceptionReviver.class).permitDuplicates();
    }

    public static OTApiExceptionBinder of(Binder binder)
    {
        return new OTApiExceptionBinder(binder);
    }

    public void registerExceptionClass(Class<? extends OTApiException> klass)
    {
        final ExceptionReviver predicate = new ExceptionReviver(klass);
        mapBinder.addBinding(predicate.getMatchedType()).toInstance(predicate);
    }
}
