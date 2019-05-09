/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * Get join root.
     *
     * @param root          root to be joined
     * @param property      property upon with base the <code>ON</code> expression
     * @param <Y>           entity class to be joined (right side of join relationship)
     *
     * @return joined entity root
     */
    <Y> Root<X> join(@NonNull Root<Y> root, @NonNull Property<X, Y> property);


    /**
     * Get <code>IS NULL</code> expression for a property
     *
     * @param property      entity property
     * @return expression
     */
    <T> Expression isNull(@NonNull SingleProperty<X, T> property);


    /**
     * Get <code>EQUALS</code> expression between a property and a value
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression eq(@NonNull SingleProperty<X, T> property, @Nullable T value);


    /**
     * Get <code>EQUALS</code> expression between two properties
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression eq(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get <code>GREATER THAN</code> expression between a property and a value
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression gt(@NonNull SingleProperty<X, T> property, @NonNull T value);


    /**
     * Get <code>GREATER THAN</code> expression between two properties
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression gt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get <code>GREATER OR EQUALS THAN</code> expression between a property and a value
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression ge(@NonNull SingleProperty<X, T> property, @NonNull T value);


    /**
     * Get <code>GREATER OR EQUALS THAN</code> expression between two properties
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression ge(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get <code>LESS THAN</code> expression between a property and a value
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression lt(@NonNull SingleProperty<X, T> property, @NonNull T value);


    /**
     * Get <code>LESS THAN</code> expression between two properties
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression lt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get <code>LESS OR EQUALS THAN</code> expression between a property and a value
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    <T> Expression le(@NonNull SingleProperty<X, T> property, @NonNull T value);


    /**
     * Get <code>LESS OR EQUALS THAN</code> expression between two properties
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    <T> Expression le(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);

}
