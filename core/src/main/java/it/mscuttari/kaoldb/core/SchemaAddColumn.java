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

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static it.mscuttari.kaoldb.core.SQLiteUtils.getColumnStatement;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getTablePrimaryKeys;
import static it.mscuttari.kaoldb.core.StringUtils.escape;
import static it.mscuttari.kaoldb.core.StringUtils.implode;

/**
 * Database schema changer: add a new column to a table.
 *
 * If the column to be added is not a primary key, it is sufficient to add it.
 * If instead, the column will have to be a primary key, the table must be totally recreated,
 * because SQLite doesn't support the add of new primary keys. In order to do that, the table is
 * renamed and a new one, containing the new column, is created. Then, the data is copied and the
 * older table is deleted.
 */
public final class SchemaAddColumn extends SchemaBaseAction {

    @NonNull private final String table;            // Table name
    @NonNull private final String name;             // New column name
    @NonNull private final Class<?> type;           // New column type
    private final boolean nullable;                 // Whether the new column should be nullable
    @Nullable private final String defaultValue;    // New column default value
    private final boolean primaryKey;               // Whether the new column should be a primary key


    /**
     * Constructor.
     *
     * @param table         table name
     * @param name          new column: name
     * @param type          new column: type
     * @param nullable      new column: nullable property
     * @param defaultValue  new column: default value
     * @param primaryKey    new column: primary key property
     *
     * @throws IllegalArgumentException if <code>table</code> or <code>name</code> are empty
     */
    public SchemaAddColumn(@NonNull String table,
                           @NonNull String name,
                           @NonNull Class<?> type,
                           boolean nullable,
                           @Nullable String defaultValue,
                           boolean primaryKey) {

        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Column name can't be empty");
        }

        this.table = table;
        this.name = name;
        this.type = type;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
        this.primaryKey = primaryKey;
    }


    @Override
    void run(SQLiteDatabase db) {
        String columnStatement = escape(name) + " " + BaseColumnObject.classToDbType(type);

        if (!nullable) {
            columnStatement += " NOT NULL";
        }

        if (defaultValue != null && !defaultValue.isEmpty()) {
            columnStatement += " DEFAULT " + escape(defaultValue);
        }

        if (!primaryKey) {
            // The column can be directly added to the existing ones

            String sql = "ALTER TABLE " + escape(table) + " ADD " + columnStatement;
            log(sql);
            db.execSQL(sql);

        } else {
            // It is not possible to add a primary key once the table has been created.
            // Therefore, the table must be recreated.

            List<String> oldColumns = getTableColumns(db, table);
            List<String> columns = getTableColumns(db, table);
            List<String> primaryKeys = getTablePrimaryKeys(db, table);
            List<String> columnsStatements = new ArrayList<>(columns.size() + 1);

            for (String column : columns) {
                columnsStatements.add(getColumnStatement(db, table, column));
            }

            columns.add(name);
            columnsStatements.add(columnStatement);

            // Set the primary keys
            primaryKeys.add(name);
            columnsStatements.add(
                    "PRIMARY KEY(" +
                    implode(primaryKeys, StringUtils::escape, ",") +
                    ")"
            );

            // Backup old table
            String tempTable = getTemporaryTableName(db);
            SchemaBaseAction renamer = new SchemaRenameTable(table, tempTable);
            renamer.run(db);

            // Create the new table containing the new column
            String newTableSql = "CREATE TABLE " + escape(table) + "(" +
                    implode(columnsStatements, statement -> statement, ",") +
                    ")";

            log(newTableSql);
            db.execSQL(newTableSql);

            // Copy data from the old table
            String dataCopySql = "INSERT INTO " + escape(table) +
                    "(" + implode(oldColumns, StringUtils::escape, ",") + ") " +
                    "SELECT " + implode(oldColumns, StringUtils::escape, ",") +
                    " FROM " + escape(tempTable);

            log(dataCopySql);
            db.execSQL(dataCopySql);

            // Delete the old table
            SchemaBaseAction deleter = new SchemaDeleteTable(tempTable);
            deleter.run(db);
        }
    }

}
