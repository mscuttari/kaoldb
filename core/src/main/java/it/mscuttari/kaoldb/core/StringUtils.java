/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.core;

import java.util.Collection;

import androidx.annotation.Nullable;

class StringUtils {

    private StringUtils() {

    }


    /**
     * Convert a collection to a string representation where each element is separated by a given string.
     * <p>A custom object-to-string converter, implementing {@link StringConverter}, can be specified
     * in order to get a define a temporary {@link Object#toString()} method. If not specified, the
     * default {@link Object#toString()} implementation of each object is used.<br>
     * If the separator is set to <code>null</code>, a comma is placed between the elements.</p>
     *
     * @param objs      objects collection
     * @param converter convert to be used to get the string representation of each object
     * @param separator separator to be used between the elements
     * @param <T>       objects type
     *
     * @return string representation
     */
    public static <T> String implode(Collection<T> objs,
                                     @Nullable StringConverter<T> converter,
                                     @Nullable String separator) {

        if (objs == null || objs.size() == 0)
            return "";

        if (converter == null)
            converter = Object::toString;

        if (separator == null)
            separator = ",";

        StringBuilder sb = new StringBuilder();
        String sep = "";

        for (T obj : objs) {
            sb.append(sep).append(converter.convert(obj));
            sep = separator;
        }

        return sb.toString();
    }


    /**
     * Interface to be used to create a custom object-to-string converter.
     *
     * @param <T> object type
     */
    public interface StringConverter<T> {

        /**
         * Called then an object has to be converted to string.
         *
         * @param obj   object to be converted
         * @return string conversion
         */
        String convert(T obj);
    }

}
