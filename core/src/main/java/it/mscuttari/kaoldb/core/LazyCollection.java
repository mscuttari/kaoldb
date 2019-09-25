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

import android.os.Build;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import it.mscuttari.kaoldb.interfaces.Query;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class allows to temporarily skip the relationships data load and run the associated queries
 * only when needed
 *
 * @param <T>   entity class
 * @param <S>   container type (e.g. {@link List})
 */
abstract class LazyCollection<T, S extends Collection<T>> implements Collection<T> {

    /**
     * It is the container where the data is stored after being loaded.
     * The container can be provided by the user when initializing the relationship field.
     * If not provided, the subclassing should fallback using a default one.
     */
    private final S data;

    /** Query to be run to get the data from the database */
    private final Query<T> query;

    /** Whether the query has been run or not */
    private boolean initialized = false;


    /**
     * Constructor.
     *
     * @param container     data container specified by the user
     * @param query         {@link Query query} to be executed to load data
     */
    protected LazyCollection(@NonNull S container, @NonNull Query<T> query) {
        this.data  = checkNotNull(container);
        this.query = checkNotNull(query);
    }


    /**
     * Get data container.
     *
     * @return data container (may be not initialized yet)
     */
    protected S getContainer() {
        return data;
    }


    /**
     * Check if the data has been loaded at least once.
     * If not, it loads it by running the query.
     */
    protected void checkInitialization() {
        if (!initialized)
            initialize();
    }


    /**
     * Load the data by running the query.
     */
    private void initialize() {
        initialized = true;
        data.clear();

        if (query != null)
            data.addAll(query.getResults());
    }


    @Override
    public int hashCode() {
        checkInitialization();
        return data.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LazyCollection))
            return false;

        checkInitialization();
        return data.equals(((LazyCollection) obj).data);
    }


    @NonNull
    @Override
    public String toString() {
        checkInitialization();
        return data.toString();
    }


    @Override
    public int size() {
        checkInitialization();
        return data.size();
    }


    @Override
    public boolean isEmpty() {
        checkInitialization();
        return data.isEmpty();
    }


    @Override
    public boolean contains(Object o) {
        checkInitialization();
        return data.contains(o);
    }


    @NonNull
    @Override
    public Iterator<T> iterator() {
        checkInitialization();
        return data.iterator();
    }


    @RequiresApi(Build.VERSION_CODES.N)
    @Override
    public void forEach(Consumer<? super T> action) {
        checkInitialization();
        data.forEach(action);
    }


    @NonNull
    @Override
    public Object[] toArray() {
        checkInitialization();
        return data.toArray();
    }


    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull T1[] a) {
        checkInitialization();
        return data.toArray(a);
    }


    @Override
    public boolean add(T t) {
        checkInitialization();
        return data.add(t);
    }


    @Override
    public boolean remove(Object o) {
        checkInitialization();
        return data.remove(o);
    }


    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        checkInitialization();
        return data.containsAll(c);
    }


    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        checkInitialization();
        return data.addAll(c);
    }


    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        checkInitialization();
        return data.retainAll(c);
    }


    @RequiresApi(Build.VERSION_CODES.N)
    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        checkInitialization();
        return data.removeIf(filter);
    }


    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        checkInitialization();
        return data.retainAll(c);
    }


    @Override
    public void clear() {
        checkInitialization();
        data.clear();
    }


    @RequiresApi(Build.VERSION_CODES.N)
    @Override
    public Spliterator<T> spliterator() {
        checkInitialization();
        return data.spliterator();
    }


    @RequiresApi(Build.VERSION_CODES.N)
    @Override
    public Stream<T> stream() {
        checkInitialization();
        return data.stream();
    }


    @RequiresApi(Build.VERSION_CODES.N)
    @Override
    public Stream<T> parallelStream() {
        checkInitialization();
        return data.parallelStream();
    }

}
