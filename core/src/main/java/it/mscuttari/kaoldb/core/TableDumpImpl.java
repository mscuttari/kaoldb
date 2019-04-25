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
