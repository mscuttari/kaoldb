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

import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.dump.ForeignKey;
import it.mscuttari.kaoldb.dump.SQLiteUtils;
import it.mscuttari.kaoldb.StringUtils;
import it.mscuttari.kaoldb.interfaces.SchemaAction;

import static it.mscuttari.kaoldb.dump.SQLiteUtils.getColumnStatement;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableForeignKeys;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTablePrimaryKeys;
import static it.mscuttari.kaoldb.StringUtils.escape;
import static it.mscuttari.kaoldb.StringUtils.implode;

/**
 * Database schema changer: rename the column of a table.
 *
 * SQLite doesn't support the renaming of existing columns. So, in order to achieve that, the table
 * is renamed and a new one, with the column renamed, is created. Then, the data is copied and the
 * older table is deleted.
 */
public final class SchemaRenameColumn extends SchemaBaseAction implements SchemaAction {

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
    public void run(SQLiteDatabase db) {
        List<String> columns = getTableColumns(db, table);

        // Prepare the statements to be used to create the new table.
        // The statements of the old table are copied and only the column name is replaced with
        // the new one in case of name match.

        List<String> newColumnsStatements = new ArrayList<>(columns.size());

        for (String column : columns) {
            String columnStatement = getColumnStatement(db, table, column);

            if (column.equals(oldName)) {
                // Note that the statement contains the escaped column name
                newColumnsStatements.add(columnStatement.replaceFirst(escape(oldName), StringUtils.escape(newName)));

            } else {
                newColumnsStatements.add(columnStatement);
            }
        }

        // List the primary keys of the new table in a similar way to the columns statements preparation
        List<String> primaryKeys = getTablePrimaryKeys(db, table);

        newColumnsStatements.add("PRIMARY KEY(" +
                StringUtils.implode(primaryKeys, obj -> obj.equals(oldName) ? escape(newName) : escape(obj), ",") +
                ")");

        // Foreign key constraints
        for (ForeignKey constraint : getTableForeignKeys(db, table)) {
            List<String> sourceColumns = new ArrayList<>();
            List<String> destinationColumns = new ArrayList<>();

            for (String sourceColumn : constraint.sourceColumns) {
                sourceColumns.add(oldName.equals(sourceColumn) ? newName : sourceColumn);
            }

            for (String destinationColumn : constraint.destinationColumns) {
                destinationColumns.add(table.equals(constraint.destinationTable) && oldName.equals(destinationColumn) ?
                        newName : destinationColumn);
            }

            ForeignKey newConstraint = new ForeignKey(table, sourceColumns, constraint.destinationTable, destinationColumns, constraint.onUpdate, constraint.onDelete);
            newColumnsStatements.add(newConstraint.toString());
        }

        // Create new table with new column name
        String newTable = getTemporaryTableName(db);
        String newTableSql = "CREATE TABLE " + escape(newTable) +
                "(" + implode(newColumnsStatements, obj -> obj, ",") + ")";

        log(newTableSql);
        db.execSQL(newTableSql);

        // Copy data from the old table
        String oldColumns = implode(columns, StringUtils::escape, ",");
        String newColumns = implode(columns, column -> column.equals(oldName) ? StringUtils.escape(newName) : StringUtils.escape(column), ",");

        String dataCopySql = "INSERT INTO " + escape(newTable) + "(" + newColumns + ") " +
                "SELECT " + oldColumns + " FROM " + StringUtils.escape(table);

        log(dataCopySql);
        db.execSQL(dataCopySql);

        // Delete the old table and rename the new one
        new SchemaDeleteTable(table).run(db);
        new SchemaRenameTable(newTable, table).run(db);

        // Fix foreign keys of other tables
        for (String table : SQLiteUtils.getTables(db)) {
            if (table.equals(this.table)) {
                // Already covered earlier
                continue;
            }

            for (ForeignKey foreignKey : SQLiteUtils.getTableForeignKeys(db, table)) {
                if (foreignKey.destinationTable.equals(this.table) && foreignKey.destinationColumns.contains(oldName)) {
                    // TODO: fix foreign key
                }
            }
        }
    }

}
