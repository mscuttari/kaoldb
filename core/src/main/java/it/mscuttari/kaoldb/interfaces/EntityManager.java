package it.mscuttari.kaoldb.interfaces;

import java.util.List;

/**
 * Entity manager gives access to all entity related operations, such as querying or persisting objects
 */
public interface EntityManager {

    /**
     * Delete database
     *
     * @return  true if everything went fine; false otherwise
     */
    boolean deleteDatabase();


    /**
     * Get query builder
     *
     * @param   resultClass     result objects class
     * @param   <T>             result objects class
     *
     * @return  query builder
     */
    <T> QueryBuilder<T> getQueryBuilder(Class<T> resultClass);


    /**
     * Get all the entity elements
     *
     * @param   entityClass     entity class
     * @param   <T>             entity class
     *
     * @return  elements list
     */
    <T> List<T> getAll(Class<T> entityClass);


    /**
     * Persist the object in the database
     *
     * @param   obj     object to be persisted
     */
    void persist(Object obj);


    /**
     * Persist the object in the database while listening to the pre-persist and post-persist events
     *
     * @param   obj             object to be persisted
     * @param   prePersist      pre-persist listener
     * @param   postPersist     post-persist listener
     * @param   <T>             object class
     */
    <T> void persist(T obj, PrePersistListener<T> prePersist, PostPersistListener<T> postPersist);

}
