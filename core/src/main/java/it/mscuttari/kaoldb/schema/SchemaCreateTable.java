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

import it.mscuttari.kaoldb.interfaces.SchemaAction;
import it.mscuttari.kaoldb.mapping.BaseColumnObject;

import static it.mscuttari.kaoldb.StringUtils.escape;

/**
 * Database schema changer: create a new table.
 * TODO: multiple primary keys, foreign keys, etc; or: SchemaAddForeignKey
 */
public final class SchemaCreateTable extends SchemaBaseAction implements SchemaAction {

    @NonNull private final String table;
    @NonNull private final String primaryKeyName;
    @NonNull private final Class<?> primaryKeyType;

    /**
     * Constructor.
     *
     * @param table             table name
     * @param primaryKeyName    primary key name
     * @param primaryKeyType    primary key type
     *
     * @throws IllegalArgumentException if <code>table</code> or <code>primaryKeyName</code> are empty
     */
    public SchemaCreateTable(@NonNull String table,
                             @NonNull String primaryKeyName,
                             @NonNull Class<?> primaryKeyType) {

        if (table.isEmpty()) {
            throw new IllegalArgumentException("Table name can't be empty");
        }

        if (primaryKeyName.isEmpty()) {
            throw new IllegalArgumentException("Primary key name can't be empty");
        }

        this.table = table;
        this.primaryKeyName = primaryKeyName;
        this.primaryKeyType = primaryKeyType;
    }

    @Override
    public void run(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + escape(table) + "(" +
                primaryKeyName + " " + BaseColumnObject.classToDbType(primaryKeyType) + " PRIMARY KEY" +
                ")";

        log(sql);
        db.execSQL(sql);
    }

}
