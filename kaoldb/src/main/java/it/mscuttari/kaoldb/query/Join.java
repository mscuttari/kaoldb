package it.mscuttari.kaoldb.query;

import android.util.Log;

import it.mscuttari.kaoldb.ColumnObject;
import it.mscuttari.kaoldb.DatabaseObject;
import it.mscuttari.kaoldb.EntityObject;
import it.mscuttari.kaoldb.annotations.JoinColumn;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

public class Join extends From {

    private From from;

    public Join(DatabaseObject db, From from, Class<?> entityClass) {
        this(db, from, entityClass, entityClass.getSimpleName());
    }

    public Join(DatabaseObject db, From from, Class<?> entityClass, String alias) {
        super(db, entityClass, alias);
        this.from = from;
    }

    @Override
    public String toString() {
        return "(" + from.toString() + ") JOIN " + super.toString() + " ON " + on();
    }

    private String on() {
        StringBuilder result = new StringBuilder();
        String separator = "";

        for (ColumnObject column : from.entity.columns) {
            if (column.field.getType().equals(entity.entityClass)) {
                result.append(separator).append(columnWithAlias(column.name)).append("=").append(from.columnWithAlias(column.referencedColumnName));
                separator = " AND ";
            }
        }

        return result.toString();
    }

}
