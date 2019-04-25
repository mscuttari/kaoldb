package it.mscuttari.kaoldb.core;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.exceptions.DumpException;
import it.mscuttari.kaoldb.interfaces.DatabaseDump;
import it.mscuttari.kaoldb.interfaces.TableDump;

class DatabaseDumpImpl implements DatabaseDump {

    /** Map between table names and dumps */
    private final Map<String, TableDump> tables;


    /**
     * Constructor
     *
     * @param db    readable database
     */
    public DatabaseDumpImpl(SQLiteDatabase db) {
        // Get all the table names
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        tables = new HashMap<>(c.getCount(), 1);

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            // Dump each table
            String tableName = c.getString(0);
            tables.put(tableName, new TableDumpImpl(db, tableName));
        }

        c.close();
    }


    @NonNull
    @Override
    public String toString() {
        return "{" +
                StringUtils.implode(
                        tables.keySet(),
                        obj -> obj + ": " + tables.get(obj),
                        ", "
                ) +
                "}";
    }


    @Override
    public TableDump getTable(String tableName) throws DumpException {
        if (!tables.containsKey(tableName)) {
            throw new DumpException("Table " + tableName + " not found");
        }

        return tables.get(tableName);
    }


    @Override
    public Collection<TableDump> getTables() {
        return Collections.unmodifiableCollection(tables.values());
    }

}
