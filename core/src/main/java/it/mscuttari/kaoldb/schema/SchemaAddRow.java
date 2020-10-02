package it.mscuttari.kaoldb.schema;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import it.mscuttari.kaoldb.interfaces.SchemaAction;
import it.mscuttari.kaoldb.mapping.BaseColumnObject;

/**
 * Database schema changer: add a row to a table.
 */
public final class SchemaAddRow implements SchemaAction {

    @NonNull private final String table;
    @NonNull private final String[] columns;
    @NonNull private final Object[] values;

    /**
     * Constructor.
     *
     * @param table     table name
     * @param columns   columns
     * @param values    values to be inserted in the provided columns
     */
    public SchemaAddRow(@NonNull String table,
                        @NonNull String[] columns,
                        @NonNull Object[] values) {

        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        for (String column : columns) {
            if (column == null || column.isEmpty()) {
                throw new IllegalArgumentException("Column name can't be empty");
            }
        }

        if (columns.length != values.length) {
            throw new IllegalArgumentException("Columns and values size mismatch");
        }

        this.table = table;
        this.columns = columns;
        this.values = values;
    }

    @Override
    public void run(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();

        for (int i = 0; i < columns.length; i++) {
            BaseColumnObject.insertIntoContentValues(cv, columns[i], values[i]);
        }

        db.insert(table, null, cv);
    }

}
