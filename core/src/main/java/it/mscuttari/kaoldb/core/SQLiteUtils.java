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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static it.mscuttari.kaoldb.core.StringUtils.escape;

class SQLiteUtils {

    private SQLiteUtils() {

    }

    /**
     * Get the tables of a database.
     *
     * @param db    readable database
     * @return database tables
     */
    public static List<String> getTables(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)) {
            List<String> tables = new ArrayList<>(c.getCount());

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                tables.add(c.getString(0));
            }

            return tables;
        }
    }

    /**
     * Get the columns of a table.
     *
     * @param db        readable database
     * @param table     table name
     *
     * @return table columns
     *
     * @throws IllegalArgumentException if the table doesn't exist
     */
    public static List<String> getTableColumns(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + escape(table) + ")", null)) {
            int nameIndex = c.getColumnIndex("name");

            if (c.getCount() == 0) {
                throw new IllegalArgumentException("Table \"" + table + "\" not found");
            }

            List<String> columns = new ArrayList<>(c.getCount());

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                columns.add(c.getString(nameIndex));
            }

            return columns;
        }
    }

    /**
     * Get the primary keys of a table.
     *
     * @param db        readable database
     * @param table     table name
     *
     * @return table primary keys
     *
     * @throws IllegalArgumentException if the table doesn't exist
     */
    public static List<String> getTablePrimaryKeys(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + escape(table) + ")", null)) {
            int nameIndex = c.getColumnIndex("name");
            int primaryKeyIndex = c.getColumnIndex("pk");

            if (c.getCount() == 0) {
                throw new IllegalArgumentException("Table \"" + table + "\" not found");
            }

            List<String> primaryKeys = new ArrayList<>(c.getCount());

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                if (c.getInt(primaryKeyIndex) == 1) {
                    primaryKeys.add(c.getString(nameIndex));
                }
            }

            return primaryKeys;
        }
    }

    /**
     * Get the statement to be used to create a column of a table.
     * <p>Example: <code>"column_name" TEXT NOT NULL DEFAULT VALUE "test"</code></p>
     *
     * @param db        readable database
     * @param table     table the column belongs to
     * @param column    column name
     *
     * @return statement to be used to create the column
     *
     * @throws IllegalArgumentException if the table doesn't exist
     * @throws IllegalArgumentException if the column doesn't exist
     */
    public static String getColumnStatement(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + escape(table) + ")", null)) {
            if (c.getCount() == 0) {
                throw new IllegalArgumentException("Table \"" + table + "\" not found");
            }

            int nameIndex = c.getColumnIndex("name");

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                if (c.getString(nameIndex).equals(column)) {
                    // Name and type
                    String result = escape(c.getString(nameIndex)) + " " + c.getString(c.getColumnIndex("type"));

                    // Nullable property
                    if (c.getInt(c.getColumnIndex("notnull")) == 1) {
                        result += " NOT NULL";
                    }

                    // Default value
                    int defaultValueIndex = c.getColumnIndex("dflt_value");
                    if (!c.isNull(defaultValueIndex)) {
                        result += " DEFAULT " + c.getString(defaultValueIndex);
                    }

                    return result;
                }
            }

            throw new IllegalArgumentException("Column \"" + column + "\" not found in table \"" + table + "\"");
        }
    }

    /**
     * Check if a column is nullable.
     *
     * @param db        readable database
     * @param table     table name
     * @param column    column name
     *
     * @return <code>true</code> if the column can be <code>null</code>; <code>false</code> otherwise
     *
     * @throws IllegalArgumentException if the table doesn't exist
     * @throws IllegalArgumentException if the column doesn't exist
     */
    public static boolean isColumnNullable(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + escape(table) + ")", null)) {
            if (c.getCount() == 0) {
                throw new IllegalArgumentException("Table \"" + table + "\" not found");
            }

            int nameIndex = c.getColumnIndex("name");
            int nullableIndex = c.getColumnIndex("notnull");

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                if (c.getString(nameIndex).equals(column)) {
                    return c.getInt(nullableIndex) == 0;
                }
            }

            throw new IllegalArgumentException("Column \"" + column + "\" not found in table \"" + table + "\"");
        }
    }

    /**
     * Get column default value.
     *
     * @param db        readable database
     * @param table     table name
     * @param column    column name
     *
     * @return default value (can be <code>null</code>)
     *
     * @throws IllegalArgumentException if the table doesn't exist
     * @throws IllegalArgumentException if the column doesn't exist
     */
    @Nullable
    public static String getColumnDefaultValue(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + escape(table) + ")", null)) {
            if (c.getCount() == 0) {
                throw new IllegalArgumentException("Table \"" + table + "\" not found");
            }

            int nameIndex = c.getColumnIndex("name");
            int defaultValueIndex = c.getColumnIndex("dflt_value");

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                if (c.getString(nameIndex).equals(column)) {
                    String result;

                    if (c.isNull(defaultValueIndex)) {
                        result = null;

                    } else {
                        result = c.getString(defaultValueIndex);
                    }

                    return result;
                }
            }

            throw new IllegalArgumentException("Column \"" + column + "\" not found in table \"" + table + "\"");
        }
    }

    /**
     * Check if a column is a primary key.
     *
     * @param db        readable database
     * @param table     table name
     * @param column    column name
     *
     * @return <code>true</code> if the column belongs to a primary key; <code>false</code> otherwise
     *
     * @throws IllegalArgumentException if the table doesn't exist
     * @throws IllegalArgumentException if the column doesn't exist
     */
    public static boolean isColumnPrimaryKey(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + escape(table) + ")", null)) {
            if (c.getCount() == 0) {
                throw new IllegalArgumentException("Table \"" + table + "\" not found");
            }

            int nameIndex = c.getColumnIndex("name");
            int pkIndex = c.getColumnIndex("pk");

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                if (c.getString(nameIndex).equals(column)) {
                    return c.getInt(pkIndex) == 1;
                }
            }

            throw new IllegalArgumentException("Column \"" + column + "\" not found in table \"" + table + "\"");
        }
    }

}
