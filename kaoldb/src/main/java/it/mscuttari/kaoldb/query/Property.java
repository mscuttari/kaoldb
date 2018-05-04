package it.mscuttari.kaoldb.query;

public class Property {

    private Class<?> entityClass;
    private String fieldName;

    public Property(Class<?> entityClass, String fieldName) {
        this.fieldName = fieldName;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getFieldName() {
        return fieldName;
    }

}
