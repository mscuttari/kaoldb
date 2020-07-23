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

package it.mscuttari.kaoldb.schema;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import static it.mscuttari.kaoldb.StringUtils.escape;

/**
 * Database schema changer: rename a table.
 */
public final class SchemaRenameTable extends SchemaBaseAction {

    @NonNull private final String oldName;
    @NonNull private final String newName;

    /**
     * Constructor.
     *
     * @param oldName   old table name
     * @param newName   new table name
     *
     * @throws IllegalArgumentException if <code>oldName</code> or <code>newName</code> are empty
     */
    public SchemaRenameTable(@NonNull String oldName,
                             @NonNull String newName) {

        if (oldName.isEmpty()) {
            throw new IllegalArgumentException("Old table name can't be empty");
        }

        if (newName.isEmpty()) {
            throw new IllegalArgumentException("New table name can't be empty");
        }

        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    void run(SQLiteDatabase db) {
        String sql = "ALTER TABLE " + escape(oldName) + " RENAME TO " + escape(newName);
        log(sql);
        db.execSQL(sql);
    }

}
