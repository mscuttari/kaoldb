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

import java.util.ArrayList;
import java.util.List;

import static it.mscuttari.kaoldb.core.SQLiteUtils.getColumnStatement;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getTablePrimaryKeys;
import static it.mscuttari.kaoldb.core.StringUtils.escape;

/**
 * Database schema changer: rename the column of a table.
 *
 * SQLite doesn't support the renaming of existing columns. So, in order to achieve that, the table
 * is renamed and a new one, with the column renamed, is created. Then, the data is copied and the
 * older table is deleted.
 */
public final class SchemaRenameColumn extends SchemaBaseAction {

    @NonNull private final String table;
    @NonNull private final String oldName;
    @NonNull private final String newName;


    /**
     * Constructor.
     *
     * @param table     table name
     * @param oldName   column to be renamed
     * @param newName   new column name to be assigned
     *
     * @throws IllegalArgumentException if <code>table</code>, <code>oldName</code> or
     *                                  <code>newName</code> are empty
     */
    public SchemaRenameColumn(@NonNull String table,
                              @NonNull String oldName,
                              @NonNull String newName) {

        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        if (oldName.isEmpty()) {
            throw new IllegalArgumentException("Old column name can't be empty");
        }

        if (newName.isEmpty()) {
            throw new IllegalArgumentException("New column name can't be empty");
        }

        this.table = table;
        this.oldName = oldName;
        this.newName = newName;
    }


    @Override
    void run(SQLiteDatabase db) {
        List<String> columns = getTableColumns(db, table);

        // Prepare the statements to be used to create the new table.
        // The statements of the old table are copied and only the column name is replaced with
        // the new one in case of name match.

        List<String> newColumnsStatements = new ArrayList<>(columns.size());

        for (String column : columns) {
            String columnStatement = getColumnStatement(db, table, column);

            if (column.equals(oldName)) {
                // Note that the statement contains the escaped column name
                newColumnsStatements.add(columnStatement.replaceFirst(escape(oldName), escape(newName)));

            } else {
                newColumnsStatements.add(columnStatement);
            }
        }

        // List the primary keys of the new table in a similar way to the columns statements preparation
        List<String> primaryKeys = getTablePrimaryKeys(db, table);

        newColumnsStatements.add("PRIMARY KEY(" +
                StringUtils.implode(primaryKeys, obj -> obj.equals(oldName) ? escape(newName) : escape(obj), ",") +
                ")");

        // Backup old table
        String tempTable = getTemporaryTableName(db);
        SchemaBaseAction renamer = new SchemaRenameTable(table, tempTable);
        renamer.run(db);

        // Create new table with new column name
        String newTableSql = "CREATE TABLE " + escape(table) + "(" +
                StringUtils.implode(newColumnsStatements, obj -> obj, ",") +
                ")";

        log(newTableSql);
        db.execSQL(newTableSql);

        // Copy data from the old table
        String oldColumns = StringUtils.implode(columns, StringUtils::escape, ",");
        String newColumns = StringUtils.implode(columns, column -> column.equals(oldName) ? escape(newName) : escape(column), ",");

        String dataCopySql = "INSERT INTO " + escape(table) + "(" + newColumns + ") " +
                "SELECT " + oldColumns + " FROM " + escape(tempTable);

        log(dataCopySql);
        db.execSQL(dataCopySql);

        // Delete the old table
        SchemaBaseAction deleter = new SchemaDeleteTable(tempTable);
        deleter.run(db);
    }

}
