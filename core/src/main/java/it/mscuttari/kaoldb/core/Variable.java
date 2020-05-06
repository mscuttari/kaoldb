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

package it.mscuttari.kaoldb.core;

import androidx.annotation.NonNull;

import it.mscuttari.kaoldb.interfaces.Root;

/**
 * This class is basically a wrapper for a property or an immediate ("raw") value.
 *
 * @param   <T>     data type
 */
class Variable<T> {

    /**
     * The same property can be used in more than once in a query.
     * Therefore a property can be distinguished from the others by referring
     * to the {@link Root} using that particular instance of the property.
     *
     * <p>The field is <code>null</code> in case of raw data.</p>
     */
    private final String tableAlias;

    /** Property involved in the expression */
    private final Property<?, T> property;

    /** Immediate value */
    private final T value;

    /**
     * Constructor for property based variable.
     *
     * @param tableAlias    table alias
     * @param property      entity property
     */
    Variable(@NonNull String tableAlias, @NonNull Property<?, T> property) {
        this.tableAlias = tableAlias;
        this.property = property;
        this.value = null;
    }

    /**
     * Constructor for immediate based variable.
     *
     * @param   value       simple object value
     */
    Variable(@NonNull T value) {
        this.tableAlias = null;
        this.property = null;
        this.value = value;
    }

    /**
     * Get table alias.
     *
     * @return  table alias
     */
    public String getTableAlias() {
        return tableAlias;
    }

    /**
     * Check if the variable has a property or raw data.
     *
     * @return <code>true</code> if {@link #property} is set; <code>false</code> otherwise
     */
    public boolean hasProperty() {
        return property != null;
    }

    /**
     * Get the property.
     * <p>Returns <code>null</code> if {@link #hasProperty()} is <code>false</code>.</p>
     *
     * @return property (<code>null</code> if the variable has raw data instead of a {@link Property property})
     */
    public Property<?, T> getProperty() {
        return property;
    }

    /**
     * Get the raw data.
     * <p>Returns <code>null</code> if {@link #hasProperty()} is <code>true</code>.</p>
     *
     * @return raw data (<code>null</code> if the variable has a {@link Property property} instead of raw data)
     */
    public T getRawData() {
        return value;
    }

}
