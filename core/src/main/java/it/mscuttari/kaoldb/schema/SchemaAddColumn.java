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

package it.mscuttari.kaoldb.schema;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

import it.mscuttari.kaoldb.interfaces.SchemaAction;

import static it.mscuttari.kaoldb.StringUtils.escape;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableForeignKeys;

/**
 * Database schema changer: add a new column to a table.
 *
 * If the column to be added is not a primary key, it is sufficient to add it.
 * If instead, the column will have to be a primary key, the table must be totally recreated,
 * because SQLite doesn't support the add of new primary keys. In order to do that, the table is
 * renamed and a new one, containing the new column, is created. Then, the data is copied and the
 * older table is deleted.
 */
public final class SchemaAddColumn extends SchemaBaseAction implements SchemaAction {

    @NonNull private final String table;
    @NonNull private final Column column;

    /**
     * Constructor.
     *
     * @param table         table name
     * @param name          new column: name
     * @param type          new column: type
     * @param defaultValue  new column: default value
     * @param primaryKey    new column: primary key property
     * @param nullable      new column: nullable property
     * @param unique        new column: unique property
     *
     * @throws IllegalArgumentException if <code>table</code> is empty
     */
    public SchemaAddColumn(@NonNull String table,
                           @NonNull String name,
                           @NonNull Class<?> type,
                           @Nullable String defaultValue,
                           boolean primaryKey,
                           boolean nullable,
                           boolean unique) {

        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Column name can't be empty");
        }

        this.table = table;
        this.column = new Column(name, type, defaultValue, primaryKey, nullable, unique);
    }

    @Override
    public void run(SQLiteDatabase db) {
        if (!column.primaryKey) {
            // The column can be directly added to the existing ones

            String sql = "ALTER TABLE " + escape(table) + " ADD " + column.getSQL();
            log(sql);
            db.execSQL(sql);

        } else {
            // It is not possible to add a primary key once the table has been created.
            // Therefore, the table must be recreated.

            // Disable foreign key checks
            db.execSQL("PRAGMA foreign_keys=OFF");

            Collection<Column> oldColumns = getTableColumns(db, table);
            Collection<Column> newColumns = getTableColumns(db, table);
            newColumns.add(column);

            Collection<ForeignKey> foreignKeys = getTableForeignKeys(db, table);

            // Create new table containing the new column
            String tempTable = getTemporaryTableName(db);
            new SchemaCreateTable(tempTable, newColumns, foreignKeys).run(db);

            // Copy data from the old table
            String columnsList = oldColumns.stream()
                    .map(column -> escape(column.name))
                    .collect(Collectors.joining(", "));

            String dataCopySql = "INSERT INTO " + escape(tempTable) + "(" + columnsList + ") " +
                    "SELECT " + columnsList + " FROM " + escape(table);

            log(dataCopySql);
            db.execSQL(dataCopySql);

            // Delete the old table and rename the temporary one
            new SchemaDeleteTable(table).run(db);
            new SchemaRenameTable(tempTable, table).run(db);

            // Enable foreign key checks
            db.execSQL("PRAGMA foreign_keys=ON");
        }
    }

}
