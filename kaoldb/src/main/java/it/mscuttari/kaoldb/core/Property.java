package it.mscuttari.kaoldb.core;

public final class Property<M, T> {

    private Class<M> entityClass;
    private Class<? super M> fieldParentClass;
    private Class<T> fieldType;
    private String fieldName;


    /**
     * Constructor
     *
     * @param   entityClass         current entity class (the parent class is set to the set of the current class)
     * @param   fieldType           field type
     * @param   fieldName           field name
     */
    public Property(Class<M> entityClass, Class<T> fieldType, String fieldName) {
        this(entityClass, entityClass, fieldType, fieldName);
    }


    /**
     * Constructor
     *
     * @param   entityClass         current entity class
     * @param   fieldParentClass    the class the property inherited from
     * @param   fieldType           field type
     * @param   fieldName           field name
     */
    public Property(Class<M> entityClass, Class<? super M> fieldParentClass, Class<T> fieldType, String fieldName) {
        this.entityClass = entityClass;
        this.fieldParentClass = fieldParentClass;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
    }


    /**
     * Get entity class
     *
     * @return  entity class
     */
    public Class<M> getEntityClass() {
        return entityClass;
    }


    /**
     * Get parent class
     *
     * @return  parent class the property originally belonged to
     */
    public Class<? super M> getFieldParentClass() {
        return fieldParentClass;
    }


    /**
     * Get field type
     *
     * @return  field class
     */
    public Class<T> getFieldType() {
        return fieldType;
    }


    /**
     * Get field name
     *
     * @return  field name
     */
    public String getFieldName() {
        return fieldName;
    }

}
