package it.mscuttari.kaoldb.core;

/**
 * @param   <M>     entity class
 * @param   <T>     data type
 */
class Variable<M, T> {

    private DatabaseObject db;
    private EntityObject entity;
    private String tableAlias;
    private Property<M, T> property;
    private T value;


    /**
     * Constructor
     *
     * @param   db          database object
     * @param   entity      entity object
     * @param   tableAlias  table alias
     * @param   property    entity property
     */
    Variable(DatabaseObject db, EntityObject entity, String tableAlias, Property<M, T> property) {
        this.db = db;
        this.entity = entity;
        this.tableAlias = tableAlias;
        this.property = property;
    }


    /**
     * Constructor
     *
     * @param   value       simple object value
     */
    Variable(T value) {
        this.value = value;
    }


    /**
     * Get database object
     *
     * @return  database object
     */
    public DatabaseObject getDatabase() {
        return db;
    }


    /**
     * Get entity object
     *
     * @return  entity object
     */
    public EntityObject getEntity() {
        return entity;
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
