package it.mscuttari.kaoldb.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    Class<X> getEntityClass();


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
    <Y> Root<X> join(@NonNull Root<Y> root, @NonNull Property<X, Y> property);


    /**
     * Get "IS NULL" expression for a field
     *
     * @param field     entity field
     *
     * @return expression
     */
    Expression isNull(@NonNull SingleProperty<X, ?> field);


    /**
     * Get "EQUALS" expression between a field and a value
     *
     * @param field     entity field
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression eq(@NonNull SingleProperty<X, T> field, @Nullable T value);


    /**
     * Get "EQUALS" expression between two fields
     *
     * @param x     first field
     * @param y     second field
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression eq(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get "EQUALS" expression between two fields
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
    <Y, T> Expression eq(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y);


    /**
     * Get "GREATER THAN" expression between a field and a value
     *
     * @param field     entity field
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression gt(@NonNull SingleProperty<X, T> field, @NonNull T value);


    /**
     * Get "GREATER THAN" expression between two fields
     *
     * @param x     first field
     * @param y     second field
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression gt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get "GREATER THAN" expression between two fields
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
    <Y, T> Expression gt(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y);


    /**
     * Get "GREATER OR EQUALS THAN" expression between a field and a value
     *
     * @param field     entity field
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression ge(@NonNull SingleProperty<X, T> field, @NonNull T value);


    /**
     * Get "GREATER OR EQUALS THAN" expression between two fields
     *
     * @param x     first field
     * @param y     second field
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression ge(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get "GREATER OR EQUALS THAN" expression between two fields
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
    <Y, T> Expression ge(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y);


    /**
     * Get "LESS THAN" expression between a field and a value
     *
     * @param field     entity field
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression lt(@NonNull SingleProperty<X, T> field, @NonNull T value);


    /**
     * Get "LESS THAN" expression between two fields
     *
     * @param x     first field
     * @param y     second field
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression lt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get "LESS THAN" expression between two fields
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
    <Y, T> Expression lt(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y);


    /**
     * Get "LESS OR EQUALS THAN" expression between a field and a value
     *
     * @param field     entity field
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression le(@NonNull SingleProperty<X, T> field, @NonNull T value);


    /**
     * Get "LESS OR EQUALS THAN" expression between two fields
     *
     * @param x     first field
     * @param y     second field
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression le(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get "LESS OR EQUALS THAN" expression between two fields
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
    <Y, T> Expression le(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y);

}
