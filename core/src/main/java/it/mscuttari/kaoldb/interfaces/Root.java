package it.mscuttari.kaoldb.interfaces;

import it.mscuttari.kaoldb.core.Property;
import it.mscuttari.kaoldb.core.SingleProperty;
import it.mscuttari.kaoldb.exceptions.QueryException;

/**
 * @param   <X>     entity class
 */
public interface Root<X> {

    /**
     * Get the entity class the root is linked to
     *
     * @return  entity class
     */
    Class<?> getEntityClass();


    /**
     * Get the alias used in the query for the current root
     *
     * @return  alias
     */
    String getAlias();


    /**
     * Get inner join root
     *
     * @param   entityClass     entity class to be joined
     * @param   property        property upon with base the "ON" expression
     * @param   <Y>             entity class to be joined (right side of join relationship)
     *
     * @return  joined entity root
     */
    <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Property<X, Y> property);


    /**
     * Get inner join root
     *
     * @param   entityClass     entity class to be joined
     * @param   alias           alias to be used for the joined entity
     * @param   on              ON expression
     * @param   <Y>             entity class to be joined (right side of join relationship)
     *
     * @return  joined entity root
     */
    <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Expression on);


    /**
     * Get left join root
     *
     * @param   entityClass     entity class to be joined
     * @param   alias           alias to be used for the joined entity
     * @param   property        property upon with base the "ON" expression
     * @param   <Y>             entity class to be joined (right side of join relationship)
     *
     * @return  joined entity root
     */
    <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Property<X, Y> property);


    /**
     * Get left join root
     *
     * @param   entityClass     entity class to be joined
     * @param   alias           alias to be used for the joined entity
     * @param   on              ON expression
     * @param   <Y>             entity class to be joined (right side of join relationship)
     *
     * @return  joined entity root
     */
    <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Expression on);


    /**
     * Get natural join root
     *
     * @param   entityClass     entity class to be joined
     * @param   alias           alias to be used for the joined entity
     * @param   <Y>             entity class to be joined (right side of join relationship)
     *
     * @return  joined entity root
     */
    <Y> Root<Y> naturalJoin(Class<Y> entityClass, String alias);


    /**
     * Get "is null" expression for a field
     *
     * @param   field       entity field
     *
     * @return  expression
     */
    Expression isNull(SingleProperty<X, ?> field);


    /**
     * Get "equals" expression between a field and a value
     *
     * @param   field       entity field
     * @param   value       value
     * @param   <T>         data type
     *
     * @return  expression
     */
    <T> Expression eq(SingleProperty<X, T> field, T value);


    /**
     * Get "equals" expression between two fields
     *
     * @param   x       first field
     * @param   y       second field
     * @param   <T>     data type
     *
     * @return  expression
     */
    <T> Expression eq(SingleProperty<X, T> x, SingleProperty<X, T> y);


    /**
     * Get "equals" expression between two fields
     *
     * @param   x           first field
     * @param   yClass      class of the second entity
     * @param   yAlias      alias of the second entity
     * @param   y           second field
     * @param   <Y>         second field belonging class
     * @param   <T>         second field data type
     *
     * @return  expression
     *
     * @throws  QueryException  if yClass is not an entity
     */
    <Y, T> Expression eq(SingleProperty<X, T> x, Class<Y> yClass, String yAlias, SingleProperty<Y, T> y);

}
