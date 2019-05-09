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
     * Constructor.
     *
     * @param db    readable database
     */
    public DatabaseDumpImpl(SQLiteDatabase db) {
        // Get all the table names
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        Map<String, TableDump> tables = new HashMap<>(c.getCount(), 1);

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            // Dump each table
            String tableName = c.getString(0);
            tables.put(tableName, new TableDumpImpl(db, tableName));
        }

        c.close();
        this.tables = Collections.unmodifiableMap(tables);
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
            throw new DumpException("Table \"" + tableName + "\" not found");
        }

        return tables.get(tableName);
    }


    @Override
    public Collection<TableDump> getTables() {
        return tables.values();
    }

}
