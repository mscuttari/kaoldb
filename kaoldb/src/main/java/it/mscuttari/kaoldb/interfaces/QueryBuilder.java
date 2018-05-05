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
    void from(Root<?> from);


    /**
     * Add "WHERE" clause
     * Multiple calls will concatenate the expressions with the "and" logical operator
     *
     * @param   expression      logic expression
     * @return  query builder
     */
    QueryBuilder where(Expression expression);


    Query<T> build(String alias);

}
