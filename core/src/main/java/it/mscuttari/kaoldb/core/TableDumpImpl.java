package it.mscuttari.kaoldb.core;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.mscuttari.kaoldb.interfaces.RowDump;
import it.mscuttari.kaoldb.interfaces.TableDump;

class TableDumpImpl implements TableDump {

    /** Table name */
    private final String name;

    /** Column names */
    private final List<String> columns;

    /** Row dumps */
    private final List<RowDump> rows;


    /**
     * Constructor
     *
     * @param db        readable database
     * @param tableName name of the table to be dumped
     */
    public TableDumpImpl(SQLiteDatabase db, String tableName) {
        this.name = tableName;

        // Get columns
        Cursor cColumns = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        columns = new ArrayList<>(cColumns.getCount());

        int columnNameIndex = cColumns.getColumnIndex("name");
        for (cColumns.moveToFirst(); !cColumns.isAfterLast(); cColumns.moveToNext()) {
            columns.add(cColumns.getString(columnNameIndex));
        }

        cColumns.close();

        // Dump the rows
        Cursor cRows = db.rawQuery("SELECT * FROM " + tableName, null);
        rows = new ArrayList<>(cRows.getCount());

        for (cRows.moveToFirst(); !cRows.isAfterLast(); cRows.moveToNext()) {
            // Dump each row
            rows.add(new RowDumpImpl(cRows));
        }

        cRows.close();
    }


    @Override
    public String toString() {
        return "[" +
                StringUtils.implode(rows, Object::toString, ", ") +
                "]";
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public List<String> getColumns() {
        return Collections.unmodifiableList(columns);
    }


    @Override
    public List<RowDump> getRows() {
        return Collections.unmodifiableList(rows);
    }

}
