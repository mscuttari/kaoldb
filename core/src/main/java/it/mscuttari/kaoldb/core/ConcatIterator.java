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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Wrapper iterator to be used to concatenate two iterators
 *
 * @param <T>   data type of the iterated objects
 */
class ConcatIterator<T> implements Iterator<T> {

    private final List<Iterable<T>> iterables;
    private Iterator<T> current;


    @SafeVarargs
    public ConcatIterator(final Iterable<T>... iterables) {
        this.iterables = new LinkedList<>(Arrays.asList(iterables));
    }


    @Override
    public boolean hasNext() {
        checkNext();
        return current != null && current.hasNext();
    }


    @Override
    public T next() {
        checkNext();
        if (current == null || !current.hasNext()) throw new NoSuchElementException();
        return current.next();
    }


    @Override
    public void remove() {
        if (current == null) throw new IllegalStateException();
        current.remove();
    }


    private void checkNext() {
        while ((current == null || !current.hasNext()) && !iterables.isEmpty()) {
            current = iterables.remove(0).iterator();
        }
    }

}
