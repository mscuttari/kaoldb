package it.mscuttari.kaoldb.schema;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.stream.Collectors;

import it.mscuttari.kaoldb.interfaces.SchemaAction;

import static it.mscuttari.kaoldb.StringUtils.escape;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableForeignKeys;

/**
 * Database schema changer: add a new foreign key constraint to a table.
 *
 * SQLite doesn't support adding a new foreign key constraint. So, in order to achieve that, the
 * table is renamed and a new one, containing the new constraint, is created. Then, the data is
 * copied and the older table is deleted.
 */
public final class SchemaAddForeignKey extends SchemaBaseAction implements SchemaAction {

    private final ForeignKey constraint;

    /**
     * Constructor.
     *
     * @param sourceTable           source table
     * @param sourceColumns         source columns
     * @param destinationTable      destination table
     * @param destinationColumns    destination columns
     * @param onUpdate              on update action
     * @param onDelete              on delete action
     */
    public SchemaAddForeignKey(@NonNull String sourceTable,
                               @NonNull String[] sourceColumns,
                               @NonNull String destinationTable,
                               @NonNull String[] destinationColumns,
                               @NonNull String onUpdate,
                               @NonNull String onDelete) {

        this.constraint = new ForeignKey(sourceTable, sourceColumns, destinationTable, destinationColumns, onUpdate, onDelete);
    }

    @Override
    public void run(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=OFF");

        Collection<Column> columns = getTableColumns(db, constraint.sourceTable);
        Collection<ForeignKey> foreignKeys = getTableForeignKeys(db, constraint.sourceTable);
        foreignKeys.add(constraint);

        // Create new table with the new foreign key constraint
        String newTable = getTemporaryTableName(db);
        new SchemaCreateTable(newTable, columns, foreignKeys).run(db);

        // Copy data into the new table
        String columnsList = columns.stream()
                .map(column -> column.name)
                .collect(Collectors.joining(", "));

        String dataCopySql = "INSERT INTO " + escape(newTable) + "(" + columnsList + ") " +
                "SELECT " + columnsList + " FROM " + escape(constraint.sourceTable);

        log(dataCopySql);
        db.execSQL(dataCopySql);

        // Delete the old table and rename the new one
        new SchemaDeleteTable(constraint.sourceTable).run(db);
        new SchemaRenameTable(newTable, constraint.sourceTable).run(db);

        db.execSQL("PRAGMA foreign_keys=ON");
    }

}
