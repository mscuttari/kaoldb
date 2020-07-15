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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.mscuttari.kaoldb.core.StringUtils.escape;
import static it.mscuttari.kaoldb.core.StringUtils.implode;

/**
 * Represents a foreign key constraint.
 * TODO: JavaDoc
 */
public final class ForeignKey {

    @NonNull public final String sourceTable;
    @NonNull public final List<String> sourceColumns;
    @NonNull public final String destinationTable;
    @NonNull public final List<String> destinationColumns;
    @NonNull public final String onUpdate;
    @NonNull public final String onDelete;

    public ForeignKey(@NonNull String sourceTable,
                      @NonNull String sourceColumn,
                      @NonNull String destinationTable,
                      @NonNull String destinationColumn,
                      @NonNull String onUpdate,
                      @NonNull String onDelete) {

        this.sourceTable = sourceTable;
        this.sourceColumns = new ArrayList<>();
        this.sourceColumns.add(sourceColumn);
        this.destinationTable = destinationTable;
        this.destinationColumns = new ArrayList<>();
        this.destinationColumns.add(destinationColumn);
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
    }

    public ForeignKey(@NonNull String sourceTable,
                      @NonNull String[] sourceColumns,
                      @NonNull String destinationTable,
                      @NonNull String[] destinationColumns,
                      @NonNull String onUpdate,
                      @NonNull String onDelete) {

        this(sourceTable, Arrays.asList(sourceColumns), destinationTable, Arrays.asList(destinationColumns), onUpdate, onDelete);
    }

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
        this.sourceColumns = new ArrayList<>(sourceColumns);
        this.destinationTable = destinationTable;
        this.destinationColumns = new ArrayList<>(destinationColumns);
        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
    }

    @NonNull
    @Override
    public String toString() {
        return "FOREIGN KEY (" + implode(sourceColumns, StringUtils::escape, ", ")
                + ") REFERENCES " + escape(destinationTable) +
                "(" + implode(destinationColumns, StringUtils::escape, ", ") + ")" +
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

}
