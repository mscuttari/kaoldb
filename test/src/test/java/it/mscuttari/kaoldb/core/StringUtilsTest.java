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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest extends AbstractTest {

    @Test
    public void implode_regular() {
        Collection<Integer> data = new ArrayList<>();
        data.add(1);
        data.add(2);
        data.add(3);

        String result = StringUtils.implode(data, Object::toString, ", ");
        assertEquals("1, 2, 3", result);
    }


    @Test
    public void implode_emptyData() {
        Collection<Integer> data = Collections.emptyList();
        String result = StringUtils.implode(data, Object::toString, ", ");
        assertEquals("", result);
    }


    @Test
    public void implode_nullData() {
        String result = StringUtils.implode(null, Object::toString, ", ");
        assertEquals("", result);
    }


    @Test
    public void implode_nullStringConverter() {
        Collection<Integer> data = new ArrayList<>();
        data.add(1);
        data.add(2);
        data.add(3);

        String result = StringUtils.implode(data, null, ", ");
        assertEquals("1, 2, 3", result);
    }


    @Test
    public void implode_nullSeparator() {
        Collection<Integer> data = new ArrayList<>();
        data.add(1);
        data.add(2);
        data.add(3);

        String result = StringUtils.implode(data, Object::toString, null);
        assertEquals("1,2,3", result);
    }

}
