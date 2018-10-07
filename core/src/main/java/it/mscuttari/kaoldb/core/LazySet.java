package it.mscuttari.kaoldb.core;

import java.util.HashSet;
import java.util.Set;

import it.mscuttari.kaoldb.interfaces.Query;

public class LazySet<T> extends LazyCollection<T, Set<T>> {

    /**
     * Constructor
     *
     * @param   container   data container specified by the user
     * @param   query       query to be executed to load data
     */
    public LazySet(Set<T> container, Query<T> query) {
        super(container == null ? new HashSet<T>() : container, query);
    }

}
