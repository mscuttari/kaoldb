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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import it.mscuttari.kaoldb.StringUtils;
import it.mscuttari.kaoldb.dump.SQLiteUtils;
import it.mscuttari.kaoldb.interfaces.SchemaAction;

import static it.mscuttari.kaoldb.StringUtils.escape;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getColumnStatement;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableForeignKeys;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTablePrimaryKeys;

/**
 * Database schema changer: delete a column from a table.
 *
 * SQLite doesn't support the removal of existing columns. So, in order to achieve that, the table
 * is renamed and a new one, with the column removed, is created. Then, the data is copied and the
 * older table is deleted.
 */
public final class SchemaDeleteColumn extends SchemaBaseAction implements SchemaAction {

    @NonNull private final String table;
    @NonNull private final String column;

    /**
     * Constructor.
     *
     * @param table     table name
     * @param column    name of the column to be dropped
     *
     * @throws IllegalArgumentException if <code>table</code> or <code>column</code> are empty
     */
    public SchemaDeleteColumn(@NonNull String table,
                              @NonNull String column) {

        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        if (column.isEmpty()) {
            throw new IllegalArgumentException("Column name can't be empty");
        }

        this.table = table;
        this.column = column;
    }

    @Override
    public void run(SQLiteDatabase db) {
        List<String> columns = SQLiteUtils.getTableColumns(db, table).stream().map(column -> column.name).collect(Collectors.toList());

        // Prepare the statements to be used to create the new table.
        // The statements of the old table are copied and only the column name is replaced with
        // the new one in case of name match.

        List<String> newColumnsStatements = new ArrayList<>(columns.size());

        for (String column : columns) {
            String columnStatement = getColumnStatement(db, table, column);

            if (!column.equals(this.column)) {
                newColumnsStatements.add(columnStatement);
            }
        }

        // List the primary keys of the new table in a similar way to the columns statements preparation
        List<String> primaryKeys = getTablePrimaryKeys(db, table);

        {
            Iterator<String> iterator = primaryKeys.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().equals(column)) {
                    iterator.remove();
                }
            }
        }

        newColumnsStatements.add("PRIMARY KEY(" +
                primaryKeys.stream().map(StringUtils::escape).collect(Collectors.joining(", ")) +
                ")");

        // Foreign key constraints
        for (ForeignKey constraint : getTableForeignKeys(db, table)) {
            List<String> sourceColumns = new ArrayList<>();
            List<String> destinationColumns = new ArrayList<>();

            for (int i = 0; i < constraint.sourceColumns.size(); i++) {
                String source = constraint.sourceColumns.get(i);
                String destination = constraint.destinationColumns.get(i);

                if (!column.equals(source) && !(table.equals(constraint.destinationTable) && column.equals(destination))) {
                    sourceColumns.add(source);
                    destinationColumns.add(destination);
                }
            }

            if (!sourceColumns.isEmpty()) {
                ForeignKey newConstraint = new ForeignKey(table, sourceColumns, constraint.destinationTable, destinationColumns, constraint.onUpdate, constraint.onDelete);
                newColumnsStatements.add(newConstraint.toString());
            }
        }

        // Create new table without the column
        String newTable = getTemporaryTableName(db);
        String newTableSql = "CREATE TABLE " + escape(newTable) +
                "(" + newColumnsStatements.stream().collect(Collectors.joining(", "))+ ")";

        log(newTableSql);
        db.execSQL(newTableSql);

        {
            // Copy data from the old table
            Iterator<String> iterator = columns.iterator();

            while (iterator.hasNext()) {
                if (iterator.next().equals(column)) {
                    iterator.remove();
                }
            }
        }

        String newColumns = columns.stream().map(StringUtils::escape).collect(Collectors.joining(", "));

        String dataCopySql = "INSERT INTO " + escape(newTable) + "(" + newColumns + ") " +
                "SELECT " + newColumns + " FROM " + escape(table);

        log(dataCopySql);
        db.execSQL(dataCopySql);

        // Delete the old table and rename the new one
        new SchemaDeleteTable(table).run(db);
        new SchemaRenameTable(newTable, table).run(db);
    }

}
