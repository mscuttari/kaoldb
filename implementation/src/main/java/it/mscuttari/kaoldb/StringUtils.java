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

package it.mscuttari.kaoldb;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

public class StringUtils {

    private StringUtils() {

    }

    /**
     * Escape the string representation of an object.
     *
     * @param obj   object whose string representation should be escaped
     * @return escaped string (<code>null</code> if <code>obj</code> is <code>null</code>)
     */
    @CheckResult
    public static String escape(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof EscapedString) {
            return obj.toString();
        }

        return escape(String.valueOf(obj));
    }

    /**
     * Escape a string.
     * <p>Every <code>"</code> character is replaced with <code>""</code> and the whole string is
     * wrapped in double quotes.</p>
     *
     * @param str   string to be escaped
     * @return escaped string (<code>null</code> if <code>str</code> is <code>null</code>)
     */
    @CheckResult
    public static String escape(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append('"');

        int length = str.length();
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

            if (c == '"') {
                sb.append('"');
            }

            sb.append(c);
        }

        sb.append('"');

        return sb.toString();
    }

    /**
     * String wrapper class used to indicate that the contained string is already escaped.
     *
     * <p>Used in the Variable class instantiation in order to avoid further quotation
     * marks in the resulting query.</p>
     */
    public static class EscapedString {

        private String value;

        public EscapedString(String value) {
            this.value = value;
        }

        @NonNull
        @Override
        public String toString() {
            return value;
        }

    }

}
