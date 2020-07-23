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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ConcatIteratorTest extends AbstractTest {

    private List<Integer> list1;
    private List<Integer> list2;
    private Iterator<Integer> iterator;

    @Before
    public void setUp() {
        list1 = new ArrayList<>();
        list2 = new ArrayList<>();
        iterator = new ConcatIterator<>(list1, list2);
    }

    @Test(expected = NoSuchElementException.class)
    public void bothEmpty() {
        assertFalse(iterator.hasNext());
        iterator.next();
    }

    @Test
    public void firstEmpty() {
        list2.add(1);
        list2.add(2);

        assertEquals(1, iterator.next().intValue());
        assertEquals(2, iterator.next().intValue());
    }


    @Test
    public void secondEmpty() {
        list1.add(1);
        list1.add(2);

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.next().intValue(), 2);
    }


    @Test
    public void bothNotEmpty() {
        list1.add(1);
        list1.add(2);
        list2.add(3);
        list2.add(4);

        assertEquals(iterator.next().intValue(), 1);
        assertEquals(iterator.next().intValue(), 2);
        assertEquals(iterator.next().intValue(), 3);
        assertEquals(iterator.next().intValue(), 4);
    }


    @Test
    public void removeFromFirst() {
        list1.add(1);
        list1.add(2);
        list2.add(3);

        iterator.next();
        iterator.next();
        iterator.remove();
        assertEquals(iterator.next().intValue(), 3);
    }


    @Test
    public void removeFromSecond() {
        list1.add(1);
        list2.add(2);
        list2.add(3);

        iterator.next();
        iterator.next();
        iterator.remove();
        assertEquals(iterator.next().intValue(), 3);
    }


    @Test(expected = IllegalStateException.class)
    public void removeWithNoNextDone() {
        iterator.remove();
    }

}
