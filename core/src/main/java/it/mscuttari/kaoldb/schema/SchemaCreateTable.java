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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import it.mscuttari.kaoldb.StringUtils;
import it.mscuttari.kaoldb.interfaces.SchemaAction;

import static it.mscuttari.kaoldb.StringUtils.escape;

/**
 * Database schema changer: create a new table.
 */
public final class SchemaCreateTable extends SchemaBaseAction implements SchemaAction {

    @NonNull
    private final String table;

    @NonNull
    private final Collection<Column> columns;

    @NonNull
    private final Collection<ForeignKey> foreignKeys;

    /**
     * Constructor.
     *
     * @param table         table name
     * @param columns       columns
     * @param foreignKeys   foreign keys
     *
     * @throws IllegalArgumentException if <code>table</code> or <code>primaryKeyName</code> are empty
     */
    public SchemaCreateTable(@NonNull String table,
                             @NonNull Collection<Column> columns,
                             @Nullable Collection<ForeignKey> foreignKeys) {

        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column");
        }

        boolean primaryKeyExistence = false;

        for (Column column : columns) {
            if (column.primaryKey) {
                primaryKeyExistence = true;
                break;
            }
        }

        if (!primaryKeyExistence) {
            throw new IllegalArgumentException("Table must have at least one primary key");
        }

        this.table = table;
        this.columns = new ArrayList<>(columns);
        this.foreignKeys = foreignKeys == null ? new ArrayList<>() : new ArrayList<>(foreignKeys);
    }

    @Override
    public void run(SQLiteDatabase db) {
        // Columns
        List<String> statements = columns.stream()
                .map(Column::getSQL)
                .collect(Collectors.toList());

        // Primary keys
        statements.add("PRIMARY KEY (" +
                columns.stream()
                .filter(column -> column.primaryKey)
                .map(column -> escape(column.name))
                .collect(Collectors.joining(", ")) +
                ")"
        );

        // Foreign keys
        statements.add(foreignKeys.stream()
                .map(constraint -> {
                    String sourceColumns = constraint.sourceColumns.stream()
                            .map(StringUtils::escape)
                            .collect(Collectors.joining(", "));

                    String destinationColumns = constraint.destinationColumns.stream()
                            .map(StringUtils::escape)
                            .collect(Collectors.joining(", "));

                    return "FOREIGN KEY (" + sourceColumns + ") REFERENCES " +
                            escape(constraint.destinationTable) + " ( " + destinationColumns + ") " +
                            "ON UPDATE " + constraint.onUpdate + " " +
                            "ON DELETE " + constraint.onDelete + " " +
                            "DEFERRABLE INITIALLY DEFERRED";
                })
                .collect(Collectors.joining(", "))
        );

        // Create the table
        String sql = "CREATE TABLE " + escape(table) + "(" +
                statements.stream()
                        .filter(statement -> !statement.isEmpty())
                        .collect(Collectors.joining(", ")) +
                ")";

        log(sql);
        db.execSQL(sql);
    }

}
