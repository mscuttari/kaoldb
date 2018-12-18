package it.mscuttari.kaoldb.core;

import androidx.annotation.NonNull;

/**
 * @param   <T>     data type
 */
class Variable<T> {

    private String tableAlias;
    private Property<?, T> property;
    private T value;


    /**
     * Constructor
     *
     * @param tableAlias    table alias
     * @param property      entity property
     */
    Variable(@NonNull String tableAlias, @NonNull Property<?, T> property) {
        this.tableAlias = tableAlias;
        this.property = property;
    }


    /**
     * Constructor
     *
     * @param   value       simple object value
     */
    Variable(@NonNull T value) {
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
     * Get the object represented by this variable.
     * It can be either an entity property or a simple raw value
     *
     * @return  data
     */
    public Object getData() {
        if (property != null) {
            return property;
        } else {
            return value;
        }
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
