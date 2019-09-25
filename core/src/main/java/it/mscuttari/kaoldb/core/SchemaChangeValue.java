/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.core;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.util.List;

import it.mscuttari.kaoldb.interfaces.DatabaseDump;
import it.mscuttari.kaoldb.interfaces.RowDump;

import static it.mscuttari.kaoldb.core.StringUtils.escape;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getTablePrimaryKeys;
import static it.mscuttari.kaoldb.core.StringUtils.implode;

/**
 * Database schema changer: for each row, change the value of a column
 * according to a user provided behaviour.
 */
public final class SchemaChangeValue<T> extends SchemaBaseAction {

    @NonNull private final String table;
    @NonNull private final String column;
    @NonNull private final SchemaChangeValueListener<T> listener;


    /**
     * Constructor.
     *
     * @param table     table name
     * @param column    column name
     * @param listener  listener handling the change
     *
     * @throws IllegalArgumentException if <code>table</code> or <code>column</code> are empty
     */
    public SchemaChangeValue(@NonNull String table,
                             @NonNull String column,
                             @NonNull SchemaChangeValueListener<T> listener) {

        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        if (column.isEmpty()) {
            throw new IllegalArgumentException("Column name can't be empty");
        }

        this.table = table;
        this.column = column;
        this.listener = listener;
    }


    @Override
    void run(SQLiteDatabase db) {
        DatabaseDump dbDump = new DatabaseDumpImpl(db);
        List<String> primaryKeys = getTablePrimaryKeys(db, table);

        try (Cursor c = db.query(table, null, null, null, null, null, null)) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                RowDump rowDump = new RowDumpImpl(c);

                ContentValues cv = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, cv);

                // Obtain the new value by applying the user provided policy
                Object value = listener.change(dbDump, rowDump, rowDump.getColumnValue(column));

                if (value instanceof Enum) {
                    cv.put(column, ((Enum) value).name());

                } else if (value instanceof Integer || value.getClass().equals(int.class)) {
                    cv.put(column, (int) value);

                } else if (value instanceof Long || value.getClass().equals(long.class)) {
                    cv.put(column, (long) value);

                } else if (value instanceof Float || value.getClass().equals(float.class)) {
                    cv.put(column, (float) value);

                } else if (value instanceof Double || value.getClass().equals(double.class)) {
                    cv.put(column, (double) value);

                } else if (value instanceof String) {
                    cv.put(column, (String) value);
                }

                db.update(
                        table,
                        cv,
                        implode(primaryKeys, pk -> {
                            // The row to be updated is identified by using its primary keys values
                            Object pkVal = rowDump.getColumnValue(pk);

                            if (pkVal instanceof String) {
                                pkVal = escape((String) pkVal);
                            }

                            return escape(pk) + "=" + pkVal;
                        }, " AND "),
                        null
                );
            }
        }
    }


    public interface SchemaChangeValueListener<T> {

        /**
         * Value change logic.
         *
         * This method is called when the old value has to be converted to the new one.
         * For user convenience, also a database dump and a row dump of the current state
         * are given.
         *
         * @param db        database dump
         * @param row       row dump
         * @param oldValue  old value
         *
         * @return new value to be stored
         */
        T change(DatabaseDump db, RowDump row, T oldValue);

    }

}
