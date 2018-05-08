package it.mscuttari.kaoldb.interfaces;

import java.util.List;

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


    void persist(Object obj);

}
