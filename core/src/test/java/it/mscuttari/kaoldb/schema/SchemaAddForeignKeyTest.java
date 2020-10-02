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

import org.junit.Test;

public class SchemaAddForeignKeyTest extends SchemaAbstractTest {

    @Override
    protected void createDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table_0 (id INTEGER PRIMARY KEY, col_1 INTEGER)");

        db.execSQL("CREATE TABLE table_1 (" +
                "id INTEGER PRIMARY KEY," +
                "col_1 INTEGER NOT NULL DEFAULT '1'," +
                "col_2 TEXT," +
                "col_3 TEXT," +
                "fk_ext1 INTEGER," +
                "fk_ext2 INTEGER," +
                "fk_int1 TEXT," +
                "fk_int2 TEXT," +
                "FOREIGN KEY (fk_ext1) REFERENCES table_0(id) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED," +
                "FOREIGN KEY (fk_int1) REFERENCES table_1(col_2) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)");
    }

    @Test
    public void add() {
        // TODO
    }

    @Test
    public void dataPersistence() {
        // TODO
    }

}
