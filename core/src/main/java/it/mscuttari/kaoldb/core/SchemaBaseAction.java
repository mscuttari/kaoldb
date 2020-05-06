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

import android.database.sqlite.SQLiteDatabase;

import it.mscuttari.kaoldb.interfaces.SchemaAction;

import static it.mscuttari.kaoldb.core.SQLiteUtils.getTables;

abstract class SchemaBaseAction implements SchemaAction {

    /**
     * Log the SQL statement that should be executed.
     *
     * @param sql   SQL statement
     */
    protected final void log(String sql) {
        LogUtils.d("[Schema change] " + sql);
    }

    /**
     * Get temporary table name that is not already used by other existing tables
     *
     * @param db    readable database
     * @return unused table name
     */
    protected final String getTemporaryTableName(SQLiteDatabase db) {
        int counter = 0;
        String name;

        do {
            name = "temp_" + counter;
            counter++;
        } while (getTables(db).contains(name));

        return name;
    }

    /**
     * Execute the change on the database.
     *
     * @param db    writable database
     */
    abstract void run(SQLiteDatabase db);

}
