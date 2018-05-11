package it.mscuttari.kaoldb.interfaces;

import java.util.List;
import java.util.concurrent.Callable;

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
     * @param   resultClass     result objects type
     * @return  query builder
     */
    <T> QueryBuilder<T> getQueryBuilder(Class<T> resultClass);


    /**
     * Get all the entity elements
     *
     * @param   entityClass     entity class
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
     */
    <M> void persist(M obj, PrePersistListener<M> prePersist, PostPersistListener<M> postPersist);

}
