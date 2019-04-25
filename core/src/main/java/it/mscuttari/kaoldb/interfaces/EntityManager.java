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

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Entity manager gives access to all entity related operations, such as querying or persisting objects
 */
public interface EntityManager {

    /**
     * Delete database
     *
     * @return true if everything went fine; false otherwise
     */
    boolean deleteDatabase();


    /**
     * Get query builder
     *
     * @param resultClass   result objects class
     * @param <T>           result objects class
     *
     * @return query builder
     */
    <T> QueryBuilder<T> getQueryBuilder(@NonNull Class<T> resultClass);


    /**
     * Get all the entity elements
     *
     * @param entityClass   entity class
     * @param <T>           entity class
     *
     * @return elements list
     */
    <T> List<T> getAll(@NonNull Class<T> entityClass);


    /**
     * Persist the object in the database
     *
     * @param obj   object to be persisted
     */
    void persist(Object obj);


    /**
     * Persist the object in the database while listening to the pre-persist and post-persist events
     *
     * @param obj           object to be persisted
     * @param prePersist    pre-persist listener
     * @param postPersist   post-persist listener
     * @param <T>           object class
     */
    <T> void persist(T obj, PreActionListener<T> prePersist, PostActionListener<T> postPersist);


    /**
     * Update an object in the database
     *
     * @param obj   object to be updated
     */
    void update(Object obj);


    /**
     * Persist the object in the database while listening to the pre-update and post-update events
     *
     * @param obj           object to be updated
     * @param preUpdate     pre-update listener
     * @param postUpdate    post-update listener
     * @param <T>           object class
     */
    <T> void update(T obj, PreActionListener<T> preUpdate, PostActionListener<T> postUpdate);


    /**
     * Remove an object from the database
     *
     * @param obj   object to be removed
     */
    void remove(Object obj);


    /**
     * Remove and object from the database while listening to the pre-update and post-update events
     *
     * @param obj           object to be removed
     * @param preRemove     pre-remove listener
     * @param postRemove    post-remove listener
     * @param <T>           object class
     */
    <T> void remove(T obj, PreActionListener<T> preRemove, PostActionListener<T> postRemove);

}
