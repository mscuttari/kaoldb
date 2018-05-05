package it.mscuttari.kaoldb.interfaces;

import it.mscuttari.kaoldb.core.Property;

public interface Root<X> {

    /**
     * Get entity class
     *
     * @return  entity class
     */
    Class<?> getEntityClass();


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
     * Get "equals" expression between a field and a value
     *
     * @param   field   entity field
     * @param   value   value
     *
     * @return  expression
     */
    Expression eq(Property field, Object value);


    /**
     * Get "equals" expression between two fields
     *
     * @param   x   first field
     * @param   y   second field
     *
     * @return  expression
     */
    Expression eq(Property x, Property y);

}
