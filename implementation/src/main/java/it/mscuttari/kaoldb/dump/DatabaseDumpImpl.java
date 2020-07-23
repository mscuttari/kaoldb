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

package it.mscuttari.kaoldb.dump;

import android.database.sqlite.SQLiteDatabase;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

import it.mscuttari.kaoldb.interfaces.DatabaseDump;
import it.mscuttari.kaoldb.interfaces.TableDump;

import static it.mscuttari.kaoldb.StringUtils.implode;

public class DatabaseDumpImpl implements DatabaseDump {

    /** Database version */
    private final int version;

    /** Map between table names and dumps */
    private final Map<String, TableDump> tables;

    /**
     * Constructor.
     *
     * @param db    readable database
     */
    public DatabaseDumpImpl(SQLiteDatabase db) {
        version = db.getVersion();

        List<String> tablesNames = SQLiteUtils.getTables(db);
        tables = new HashMap<>(tablesNames.size(), 1);

        for (String table : tablesNames) {
            tables.put(table, new TableDumpImpl(db, table));
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                implode(
                        tables.keySet(),
                        obj -> obj + ": " + tables.get(obj),
                        ", "
                ) +
                "}";
    }

    @Override
    public int getVersion() {
        return version;
    }

    @NonNull
    @Override
    public TableDump getTable(String tableName) {
        TableDump table = tables.get(tableName);

        if (table == null) {
            throw new IllegalArgumentException("Table \"" + tableName + "\" not found");
        }

        return table;
    }

    @NonNull
    @Override
    public Collection<TableDump> getTables() {
        return Collections.unmodifiableCollection(tables.values());
    }

}
