package it.mscuttari.kaoldb.core;

import androidx.annotation.NonNull;

/**
 * @param   <T>     data type
 */
class Variable<T> {

    private final String tableAlias;
    private final Property<?, T> property;
    private final T value;


    /**
     * Constructor
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
     * Constructor
     *
     * @param   value       simple object value
     */
    Variable(@NonNull T value) {
        this.tableAlias = null;
        this.property = null;
        this.value = value;
    }


    /**
     * Get table alias
     *
     * @return  table alias
     */
    public String getTableAlias() {
        return tableAlias;
    }


    /**
     * Check if the variable has a property
     *
     * @return true if {@link #property} is set; false otherwise
     */
    public boolean hasProperty() {
        return property != null;
    }


    /**
     * Get the property
     *
     * @return property
     */
    public Property<?, T> getProperty() {
        return property;
    }


    /**
     * Get the raw data
     *
     * @return raw data
     */
    public T getRawData() {
        return value;
    }


    /**
     * String wrapper class.
     * Used in the Variable class creation in order to avoid the quotation marks in the resulting query.
     */
    public static class StringWrapper {

        private String value;

        StringWrapper(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

    }

}
