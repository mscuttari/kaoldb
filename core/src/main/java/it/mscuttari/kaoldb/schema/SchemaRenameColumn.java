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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import it.mscuttari.kaoldb.StringUtils;
import it.mscuttari.kaoldb.dump.SQLiteUtils;
import it.mscuttari.kaoldb.interfaces.SchemaAction;

import static it.mscuttari.kaoldb.StringUtils.escape;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableForeignKeys;

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
        // Disable foreign key checks
        db.execSQL("PRAGMA foreign_keys=OFF");

        List<Column> columns = getTableColumns(db, table);

        // Create the new table

        List<Column> newColumns = columns
                .stream()
                .map(column -> {
                    if (column.name.equals(oldName)) {
                        return new Column(newName, column.type, column.defaultValue, column.primaryKey, column.nullable, column.unique);
                    } else {
                        return column;
                    }
                })
                .collect(Collectors.toList());

        // Foreign key constraints
        Collection<ForeignKey> newForeignKeys = getTableForeignKeys(db, table)
                .stream()
                .map(constraint -> {
                    List<String> sourceColumns = new ArrayList<>();
                    List<String> destinationColumns = new ArrayList<>();

                    for (String sourceColumn : constraint.sourceColumns) {
                        sourceColumns.add(oldName.equals(sourceColumn) ? newName : sourceColumn);
                    }

                    for (String destinationColumn : constraint.destinationColumns) {
                        if (table.equals(constraint.destinationTable) && oldName.equals(destinationColumn)) {
                            // Internal reference
                            destinationColumns.add(newName);
                        } else {
                            // External reference
                            destinationColumns.add(destinationColumn);
                        }
                    }

                    return new ForeignKey(table, sourceColumns, constraint.destinationTable, destinationColumns, constraint.onUpdate, constraint.onDelete);
                })
                .collect(Collectors.toList());

        String newTable = getTemporaryTableName(db);
        new SchemaCreateTable(newTable, newColumns, newForeignKeys).run(db);

        // Copy data from the old table
        String dataCopySql = "INSERT INTO " + escape(newTable) + "(" +
                newColumns.stream()
                        .map(column -> escape(column.name))
                        .collect(Collectors.joining(", ")) +
                ") SELECT " +
                columns.stream()
                        .map(column -> escape(column.name))
                        .collect(Collectors.joining(", ")) +
                " FROM " + StringUtils.escape(table);

        log(dataCopySql);
        db.execSQL(dataCopySql);

        // Delete the old table and rename the new one
        new SchemaDeleteTable(table).run(db);
        new SchemaRenameTable(newTable, table).run(db);

        // Fix foreign keys of other tables
        fixOtherTablesForeignKeys(db);

        // Enable foreign key checks
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    /**
     * Fix the foreign key constraints of other tables referencing the renamed column.
     *
     * @param db    writable database
     */
    private void fixOtherTablesForeignKeys(SQLiteDatabase db) {
        for (String table : SQLiteUtils.getTables(db)) {
            if (table.equals(this.table)) {
                // Already covered
                continue;
            }

            for (ForeignKey foreignKey : getTableForeignKeys(db, table)) {
                if (foreignKey.destinationTable.equals(this.table) && foreignKey.destinationColumns.contains(oldName)) {
                    List<Column> columns = getTableColumns(db, table);

                    Collection<ForeignKey> newForeignKeys = getTableForeignKeys(db, table)
                            .stream()
                            .map(constraint -> {
                                List<String> destinationColumns = new ArrayList<>();

                                for (String destinationColumn : constraint.destinationColumns) {
                                    if (table.equals(constraint.destinationTable) && oldName.equals(destinationColumn)) {
                                        destinationColumns.add(newName);
                                    } else {
                                        destinationColumns.add(destinationColumn);
                                    }
                                }

                                return new ForeignKey(table, constraint.sourceColumns, constraint.destinationTable, destinationColumns, constraint.onUpdate, constraint.onDelete);
                            })
                            .collect(Collectors.toList());

                    String newTable = getTemporaryTableName(db);
                    new SchemaCreateTable(newTable, columns, newForeignKeys).run(db);

                    // Copy data from the old table
                    String columnsList = columns.stream()
                            .map(column -> escape(column.name))
                            .collect(Collectors.joining(", "));

                    String dataCopySql = "INSERT INTO " + escape(newTable) + "(" + columnsList + ") " +
                            "SELECT " + columnsList + " FROM " + StringUtils.escape(table);

                    log(dataCopySql);
                    db.execSQL(dataCopySql);

                    // Delete the old table and rename the new one
                    new SchemaDeleteTable(table).run(db);
                    new SchemaRenameTable(newTable, table).run(db);
                }
            }
        }
    }
}
