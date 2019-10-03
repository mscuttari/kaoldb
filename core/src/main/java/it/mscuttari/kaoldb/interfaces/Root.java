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

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import it.mscuttari.kaoldb.core.Property;
import it.mscuttari.kaoldb.core.SingleProperty;

/**
 * Roots can be thought as a direct access to the entity table properties and allow
 * to create expressions based on their values.
 *
 * @param   <X>     entity class
 */
public interface Root<X> {

    /**
     * Get the entity class the root is linked to.
     *
     * @return entity class
     */
    @CheckResult
    @NonNull
    Class<X> getEntityClass();


    /**
     * Get the root alias to be used in the queries.
     *
     * @return alias
     */
    @CheckResult
    @NonNull
    String getAlias();


    /**
     * Get join root.
     *
     * <p>A join root allows to query multiple tables using the properties of all the
     * joined roots.</p>
     *
     * @param root          root to be joined
     * @param property      property upon with base the <code>ON</code> expression
     * @param <Y>           entity class to be joined (right side of join relationship)
     *
     * @return joined entity root
     */
    @CheckResult
    @NonNull
    <Y> Root<X> join(@NonNull Root<Y> root, @NonNull Property<X, Y> property);


    /**
     * Get all the roots that are directly joined together by the user.
     *
     * <p>The root that are dynamically joined, such as parents and children ones, are
     * not included.</p>
     *
     * @return all the roots that have been directly joined
     */
    @CheckResult
    @NonNull
    Collection<Root<?>> getJoinedRoots();


    /**
     * Get <code>IS NULL</code> expression for a property.
     *
     * @param property      entity property
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression isNull(@NonNull SingleProperty<X, T> property);


    /**
     * Get <code>EQUALS</code> expression between a property and a value.
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression eq(@NonNull SingleProperty<X, T> property, @Nullable T value);


    /**
     * Get <code>EQUALS</code> expression between two properties.
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression eq(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get <code>GREATER THAN</code> expression between a property and a value.
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression gt(@NonNull SingleProperty<X, T> property, @NonNull T value);


    /**
     * Get <code>GREATER THAN</code> expression between two properties.
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression gt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get <code>GREATER OR EQUALS THAN</code> expression between a property and a value.
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression ge(@NonNull SingleProperty<X, T> property, @NonNull T value);


    /**
     * Get <code>GREATER OR EQUALS THAN</code> expression between two properties.
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression ge(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get <code>LESS THAN</code> expression between a property and a value.
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression lt(@NonNull SingleProperty<X, T> property, @NonNull T value);


    /**
     * Get <code>LESS THAN</code> expression between two properties.
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression lt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);


    /**
     * Get <code>LESS OR EQUALS THAN</code> expression between a property and a value.
     *
     * @param property  entity property
     * @param value     value
     * @param <T>       data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression le(@NonNull SingleProperty<X, T> property, @NonNull T value);


    /**
     * Get <code>LESS OR EQUALS THAN</code> expression between two properties.
     *
     * @param x     first property
     * @param y     second property
     * @param <T>   data type
     *
     * @return expression
     */
    @CheckResult
    @NonNull
    <T> Expression le(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y);

}
