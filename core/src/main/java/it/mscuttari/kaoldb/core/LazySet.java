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

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.interfaces.Query;

/**
 * Lazy collection implementation using a {@link Set} as data container.
 *
 * @param <T>   POJO class
 */
class LazySet<T> extends LazyCollection<T, Set<T>> {

    /**
     * Constructor.
     *
     * @param container     data container specified by the user
     * @param query         {@link Query query} to be executed to load data
     */
    public LazySet(Set<T> container, @NonNull Query<T> query) {
        super(container == null ? new HashSet<>() : container, query);
    }

}
