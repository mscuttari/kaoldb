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
     * Get entity root.
     *
     * <p>Roots can be thought as a direct access to the entity table properties and allow to create
     * expressions based on their values</p>
     *
     * @param entityClass   entity class
     * @param <M>           entity class
     *
     * @return entity root
     *
     * @see Root
     */
    <M> Root<M> getRoot(@NonNull Class<M> entityClass);


    /**
     * Set the <code>FROM</code> clause for the query to be built.
     *
     * @param from      entity root
     * @return current query builder instance
     */
    QueryBuilder<T> from(Root<?> from);


    /**
     * Set the <code>WHERE</code> clause for the query to be built.
     *
     * @param where     logic expression
     * @return current query builder instance
     */
    QueryBuilder<T> where(Expression where);


    /**
     * Create the query.
     *
     * <p>Get the {@link Query} object corresponding to the specified query clauses (from, where, etc.).<br>
     * If the clauses are modified after a query build and then rebuilt, the new query will be
     * different then the previous one and will reflect the new clauses. The previously built one
     * will keep the clauses it was built with.</p>
     *
     * @param root      the root of the desired result entity
     *
     * @return {@link Query} object which can be used to retrieve query result objects
     *
     * @throws QueryException if the query configuration is invalid (see exception message for
     *                        further details)
     *
     * @see #from(Root)
     * @see #where(Expression)
     */
    Query<T> build(@NonNull Root<T> root);

}
