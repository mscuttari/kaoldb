package it.mscuttari.kaoldb.core;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import static it.mscuttari.kaoldb.core.StringUtils.escape;
import static it.mscuttari.kaoldb.core.StringUtils.implode;

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

}
