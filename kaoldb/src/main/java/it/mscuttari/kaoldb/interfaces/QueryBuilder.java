package it.mscuttari.kaoldb.interfaces;

public interface QueryBuilder<T> {

    /**
     * Get entity root
     *
     * @param   entityClass     entity class
     * @param   alias           entity alias to be used in the query
     *
     * @return  root
     */
    <M> Root<M> getRoot(Class<M> entityClass, String alias);


    /**
     * Set "FROM" clause
     *
     * @param   from    entity root
     */
    QueryBuilder<T> from(Root<?> from);


    /**
     * Set "WHERE" clause
     *
     * @param   where       logic expression
     * @return  query builder
     */
    QueryBuilder<T> where(Expression where);


    /**
     * Create the query
     *
     * @param   alias       the alias of the desired result entity
     * @return  query object
     */
    Query<T> build(String alias);

}
