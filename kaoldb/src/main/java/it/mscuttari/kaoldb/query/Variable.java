package it.mscuttari.kaoldb.query;

import it.mscuttari.kaoldb.DatabaseObject;
import it.mscuttari.kaoldb.EntityObject;

public class Variable {

    public enum VariableType {
        SIMPLE,         // Just a value
        SINGLE,         // Single column
        COMPOSITE       // Multiple columns
    }

    public VariableType type;

    private DatabaseObject db;
    private String tableAlias;
    private Property property;
    private Object value;

    public Variable(DatabaseObject db, String tableAlias, Property property) {
        this.db = db;
        this.tableAlias = tableAlias;
        this.property = property;

        EntityObject entityObject = db.entities.get(property.getEntityClass());
    }

    public Variable(Object value) {
        this.value = value;
        this.type = VariableType.SIMPLE;
    }

    @Override
    public String toString() {
        if (value == null) {
            return tableAlias + "." + property.getFieldName();
        } else {
            return String.valueOf(value);
        }
    }
}
