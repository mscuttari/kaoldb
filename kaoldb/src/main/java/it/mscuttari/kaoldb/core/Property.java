package it.mscuttari.kaoldb.core;

public final class Property {

    private Class<?> entityClass;
    private String fieldName;

    public Property(Class<?> entityClass, String fieldName) {
        this.entityClass = entityClass;
        this.fieldName = fieldName;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getFieldName() {
        return fieldName;
    }

}
