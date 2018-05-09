package it.mscuttari.kaoldb.interfaces;

import it.mscuttari.kaoldb.core.Property;
import it.mscuttari.kaoldb.exceptions.QueryException;

public interface Root<X> {

    /**
     * Get entity class
     *
     * @return  entity class
     */
    Class<?> getEntityClass();


    /**
     * Get table alias
     *
     * @return  table alias
     */
    String getTableAlias();


    /**
     * Get inner join root
     *
     * @param   entityClass     entity class to be joined
     * @param   property        property upon with base the "ON" expression
     *
     * @return  root
     */
    <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Property<X, Y> property);


    /**
     * Get inner join root
     *
     * @param   entityClass     entity class to be joined
     * @param   on              ON expression
     *
     * @return  root
     */
    <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Expression on);


    /**
     * Get left join root
     *
     * @param   entityClass     entity class to be joined
     * @param   property        property upon with base the "ON" expression
     *
     * @return  root
     */
    <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Property<X, Y> property);


    /**
     * Get left join root
     *
     * @param   entityClass     entity class to be joined
     * @param   on              ON expression
     *
     * @return  root
     */
    <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Expression on);


    /**
     * Get natural join root
     *
     * @param   entityClass     entity class to be joined
     * @return  root
     */
    <Y> Root<Y> naturalJoin(Class<Y> entityClass, String alias);


    /**
     * Get "is null" expression for a field
     *
     * @param   field   entity field
     * @return  expression
     */
    Expression isNull(Property<X, ?> field);


    /**
     * Get "equals" expression between a field and a value
     *
     * @param   field   entity field
     * @param   value   value
     *
     * @return  expression
     */
    <T> Expression eq(Property<X, T> field, T value);


    /**
     * Get "equals" expression between two fields
     *
     * @param   x   first field
     * @param   y   second field
     *
     * @return  expression
     */
    <T> Expression eq(Property<X, T> x, Property<X, T> y);


    /**
     * Get "equals" expression between two fields
     *
     * @param   x       first field
     * @param   yClass  class of the second entity
     * @param   yAlias  alias of the second entity
     * @param   y       second field
     *
     * @return  expression
     *
     * @throws  QueryException  if yClass is not an entity
     */
    <Y, T> Expression eq(Property<X, T> x, Class<Y> yClass, String yAlias, Property<Y, T> y);

}
