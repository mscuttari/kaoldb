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

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a foreign key constraint.
 */
public final class ForeignKey {

    @NonNull public final String sourceTable;
    @NonNull public final List<String> sourceColumns;
    @NonNull public final String destinationTable;
    @NonNull public final List<String> destinationColumns;
    @NonNull public final String onUpdate;
    @NonNull public final String onDelete;

    /**
     * Constructor for constraint with single column.
     *
     * @param sourceTable           source table
     * @param sourceColumn          source column
     * @param destinationTable      destination table
     * @param destinationColumn     destination column
     * @param onUpdate              on update action
     * @param onDelete              on delete action
     */
    public ForeignKey(@NonNull String sourceTable,
                      @NonNull String sourceColumn,
                      @NonNull String destinationTable,
                      @NonNull String destinationColumn,
                      @NonNull String onUpdate,
                      @NonNull String onDelete) {

        this.sourceTable = sourceTable;
        this.sourceColumns = Collections.unmodifiableList(Collections.singletonList(sourceColumn));
        this.destinationTable = destinationTable;
        this.destinationColumns = Collections.unmodifiableList(Collections.singletonList(destinationColumn));
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
    }

    /**
     * Constructor for constraint with multiple columns.
     *
     * @param sourceTable           source table
     * @param sourceColumns         source columns
     * @param destinationTable      destination table
     * @param destinationColumns    destination columns
     * @param onUpdate              on update action
     * @param onDelete              on delete action
     */
    public ForeignKey(@NonNull String sourceTable,
                      @NonNull String[] sourceColumns,
                      @NonNull String destinationTable,
                      @NonNull String[] destinationColumns,
                      @NonNull String onUpdate,
                      @NonNull String onDelete) {

        this(sourceTable, Arrays.asList(sourceColumns), destinationTable, Arrays.asList(destinationColumns), onUpdate, onDelete);
    }

    /**
     * Constructor for constraint with multiple columns.
     *
     * @param sourceTable           source table
     * @param sourceColumns         source columns
     * @param destinationTable      destination table
     * @param destinationColumns    destination columns
     * @param onUpdate              on update action
     * @param onDelete              on delete action
     */
    public ForeignKey(@NonNull String sourceTable,
                      @NonNull List<String> sourceColumns,
                      @NonNull String destinationTable,
                      @NonNull List<String> destinationColumns,
                      @NonNull String onUpdate,
                      @NonNull String onDelete) {

        if (sourceColumns.size() == 0) {
            throw new IllegalArgumentException("No source columns specified");
        }

        if (sourceColumns.size() != destinationColumns.size()) {
            throw new IllegalArgumentException("Size mismatch in foreign key constraint");
        }

        this.sourceTable = sourceTable;
        this.sourceColumns = Collections.unmodifiableList(new ArrayList<>(sourceColumns));
        this.destinationTable = destinationTable;
        this.destinationColumns = Collections.unmodifiableList(new ArrayList<>(destinationColumns));
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
    }

    @NonNull
    @Override
    public String toString() {
        String localColumns = sourceColumns.stream()
                .map(ForeignKey::escape)
                .collect(Collectors.joining(", "));

        String referencedColumns = destinationColumns.stream()
                .map(ForeignKey::escape)
                .collect(Collectors.joining(", "));

        return "FOREIGN KEY (" + localColumns
                + ") REFERENCES " + escape(destinationTable) +
                "(" + referencedColumns + ")" +
                " ON UPDATE " + onUpdate +
                " ON DELETE " + onDelete +
                " DEFERRABLE INITIALLY DEFERRED";
    }

    @Override
    public int hashCode() {
        return sourceTable.hashCode() +
                sourceColumns.hashCode() +
                destinationTable.hashCode() +
                destinationColumns.hashCode() +
                onUpdate.hashCode() +
                onDelete.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ForeignKey))
            return false;

        ForeignKey that = (ForeignKey) obj;

        return sourceTable.equals(that.sourceTable) &&
                sourceColumns.containsAll(that.sourceColumns) &&
                that.sourceColumns.containsAll(sourceColumns) &&
                destinationTable.equals(that.destinationTable) &&
                destinationColumns.containsAll(that.destinationColumns) &&
                that.destinationColumns.containsAll(destinationColumns) &&
                onUpdate.equals(that.onUpdate) &&
                onDelete.equals(that.onDelete);
    }

    /**
     * Get a new constraint starting from the current one and by adding a column.
     * The current constraint is left untouched.
     *
     * @param sourceColumn          source column to be added
     * @param destinationColumn     destination column to be added
     *
     * @return extended foreign key constraint
     */
    @CheckResult
    public ForeignKey addColumn(String sourceColumn, String destinationColumn) {
        List<String> sourceColumns = new ArrayList<>(this.sourceColumns);
        sourceColumns.add(sourceColumn);

        List<String> destinationColumns = new ArrayList<>(this.destinationColumns);
        destinationColumns.add(destinationColumn);

        return new ForeignKey(sourceTable, sourceColumns, destinationTable, destinationColumns, onUpdate, onDelete);
    }

    @CheckResult
    private static String escape(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append('"');

        int length = str.length();
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

            if (c == '"') {
                sb.append('"');
            }

            sb.append(c);
        }

        sb.append('"');

        return sb.toString();
    }

}
