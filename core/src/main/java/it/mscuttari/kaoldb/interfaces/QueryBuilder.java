package it.mscuttari.kaoldb.interfaces;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.exceptions.QueryException;

/**
 * Provide methods to build each part of a common SQL query
 *
 * @param   <T>     result objects class
 */
public interface QueryBuilder<T> {

    /**
     * Get entity root
     *
     * Roots can be thought as a direct access to the entity table properties and allow to create
     * expressions based on their values
     *
     * @param entityClass   entity class
     * @param <M>           entity class
     *
     * @return entity root
     */
    <M> Root<M> getRoot(@NonNull Class<M> entityClass);


    /**
     * Set "FROM" clause
     *
     * @param from      entity root
     */
    QueryBuilder<T> from(Root<?> from);


    /**
     * Set "WHERE" clause
     *
     * @param where     logic expression
     *
     * @return query builder
     */
    QueryBuilder<T> where(Expression where);


    /**
     * Create the query
     *
     * Get the {@link Query} object corresponding to the specified query clauses (from, where, etc.).
     * If the clauses are modified after a query build and then rebuilt, the new query will be
     * different then the previous one and will reflect the new clauses
     *
     * @param root      the root of the desired result entity
     *
     * @return {@link Query} object which can be used to retrieve query result objects
     *
     * @throws QueryException if the query configuration is invalid (see exception message for
     *                        further details)
     */
    Query<T> build(@NonNull Root<T> root);

}
