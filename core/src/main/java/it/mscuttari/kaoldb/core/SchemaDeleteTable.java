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

import androidx.annotation.NonNull;

import static it.mscuttari.kaoldb.core.StringUtils.escape;

/**
 * Database schema changer: delete a table.
 */
public final class SchemaDeleteTable extends SchemaBaseAction {

    @NonNull private final String table;


    /**
     * Constructor.
     *
     * @param table     name of the table to be deleted
     * @throws IllegalArgumentException if <code>table</code> is empty
     */
    public SchemaDeleteTable(@NonNull String table) {
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        this.table = table;
    }


    @Override
    void run(SQLiteDatabase db) {
        String sql = "DROP TABLE " + escape(table);
        log(sql);
        db.execSQL(sql);
    }

}
