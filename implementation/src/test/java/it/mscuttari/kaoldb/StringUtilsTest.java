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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringUtilsTest extends AbstractTest {

    @Test
    public void escape_string() {
        assertEquals("\"Test\"", StringUtils.escape("Test"));
        assertEquals("\"\"\"Test\"", StringUtils.escape("\"Test"));
    }

    @Test
    public void escape_nullString() {
        assertNull(StringUtils.escape(null));
    }

    @Test
    public void escape_bypass() {
        StringUtils.EscapedString escapedString = new StringUtils.EscapedString("\"Test\"");
        assertEquals("\"Test\"", StringUtils.escape(escapedString));
    }

    @Test
    public void escape_object() {
        assertEquals("\"1\"", StringUtils.escape(1));
    }

    @Test
    public void escape_nullObject() {
        assertNull(StringUtils.escape((Object) null));
    }

}
