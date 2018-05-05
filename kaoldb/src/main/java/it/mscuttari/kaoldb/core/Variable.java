package it.mscuttari.kaoldb.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.QueryException;

class Variable {

    private DatabaseObject db;
    private EntityObject entity;
    private String tableAlias;
    private Property property;
    private Object value;


    Variable(DatabaseObject db, EntityObject entity, String tableAlias, Property property) {
        this.db = db;
        this.entity = entity;
        this.tableAlias = tableAlias;
        this.property = property;

        /*try {
            Field field = entity.entityClass.getField(property.getFieldName());

            if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(JoinColumn.class)) {
                this.type = VariableType.SINGLE;
            } else if (field.isAnnotationPresent(JoinColumns.class) || field.isAnnotationPresent(JoinTable.class)) {
                this.type = VariableType.COMPOSITE;
            }

        } catch (NoSuchFieldException e) {
            throw new QueryException("Field " + property.getFieldName() + " not found in entity " + entity.entityClass.getSimpleName());
        }*/
    }

    Variable(Object value) {
        this.value = value;
    }

    public EntityObject getEntity() {
        return entity;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public Object getData() {
        if (property != null) {
            return property;
        } else {
            return value;
        }
    }

    /*@Override
    public String toString() {
        if (value == null) {
            return tableAlias + "." + property.getFieldName();

        } else {
            if (value instanceof String) {
                return "\"" + value + "\"";
            } else {
                return String.valueOf(value);
            }
        }
    }*/

}
