package it.mscuttari.kaoldb.core;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.interfaces.Query;

/**
 * Lazy collection implementation using a {@link Set} as data container
 *
 * @param <T>   POJO class
 */
class LazySet<T> extends LazyCollection<T, Set<T>> {

    /**
     * Constructor
     *
     * @param container     data container specified by the user
     * @param query         query to be executed to load data
     */
    public LazySet(Set<T> container, @NonNull Query<T> query) {
        super(container == null ? new HashSet<>() : container, query);
    }

}
