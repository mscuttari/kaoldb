package it.mscuttari.kaoldb.core;

public final class Property<M, T> {

    private Class<M> entityClass;
    private Class<T> fieldType;
    private String fieldName;

    public Property(Class<M> entityClass, Class<T> fieldType, String fieldName) {
        this.entityClass = entityClass;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
    }

    public Class<M> getEntityClass() {
        return entityClass;
    }

    public Class<T> getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

}
