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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.mscuttari.kaoldb.query.CachedCursor;
import it.mscuttari.kaoldb.StringUtils;
import it.mscuttari.kaoldb.interfaces.RowDump;
import it.mscuttari.kaoldb.interfaces.TableDump;

public class TableDumpImpl implements TableDump {

    /** Table name */
    private final String name;

    /** Query to recreate the table */
    private String sql;

    /** Column names */
    private final List<String> columns;

    /** Row dumps */
    private final List<RowDump> rows;

    /**
     * Dump the content of a table.
     *
     * @param db        readable database
     * @param tableName name of the table to be dumped
     */
    public TableDumpImpl(SQLiteDatabase db, String tableName) {
        this.name = tableName;

        // Get create SQL
        try (Cursor cSql = db.query("sqlite_master", new String[] {"sql"}, "name = ?", new String[] {tableName}, null, null, null)) {
            cSql.moveToFirst();
            sql = cSql.getString(0);
        }

        // Get columns
        columns = SQLiteUtils.getTableColumns(db, tableName);

        // Dump the rows
        try (Cursor cRows = new CachedCursor(db.rawQuery("SELECT * FROM " + tableName, null))) {
            rows = new ArrayList<>(cRows.getCount());

            for (cRows.moveToFirst(); !cRows.isAfterLast(); cRows.moveToNext()) {
                rows.add(new RowDumpImpl(cRows));
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + StringUtils.implode(rows, Object::toString, ", ") + "]";
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String getSql() {
        return sql;
    }

    @NonNull
    @Override
    public List<String> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    @NonNull
    @Override
    public List<RowDump> getRows() {
        return Collections.unmodifiableList(rows);
    }

}
