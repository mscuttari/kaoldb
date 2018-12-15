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
     * @return entity class
     */
    Class<?> getEntityClass();


    /**
     * Get the alias used in the query for the current root
     *
     * @return alias
     */
    String getAlias();


    /**
     * Get full alias of this entity table (alias + class name)
     *
     * @return full alias
     */
    String getFullAlias();


    /**
     * Get join root
     *
     * @param root          root to be joined
     * @param property      property upon with base the "ON" expression
     * @param <Y>           entity class to be joined (right side of join relationship)
     *
     * @return joined entity root
     */
    <Y> Root<X> join(Root<Y> root, Property<X, Y> property);


    /**
     * Get "is null" expression for a field
     *
     * @param field     entity field
     *
     * @return expression
     */
    Expression isNull(SingleProperty<X, ?> field);


    /**
     * Get "equals" expression between a field and a value
     *
     * @param field     entity field
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression eq(SingleProperty<X, T> field, T value);


    /**
     * Get "equals" expression between two fields
     *
     * @param x     first field
     * @param y     second field
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression eq(SingleProperty<X, T> x, SingleProperty<X, T> y);


    /**
     * Get "equals" expression between two fields
     *
     * @param x         first field
     * @param yClass    class of the second entity
     * @param yAlias    alias of the second entity
     * @param y         second field
     * @param <Y>       second field belonging class
     * @param <T>       second field data type
     *
     * @return expression
     *
     * @throws QueryException if yClass is not an entity
     */
    <Y, T> Expression eq(SingleProperty<X, T> x, Class<Y> yClass, String yAlias, SingleProperty<Y, T> y);

}
