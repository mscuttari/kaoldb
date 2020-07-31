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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Test;

import java.util.Collection;

import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTables;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class SchemaRenameTableTest extends SchemaAbstractTest {

    @Override
    protected void createDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table_1 (" +
                "col_1 INTEGER PRIMARY KEY, " +
                "col_2 REAL, " +
                "col_3 TEXT)");

        db.execSQL("INSERT INTO table_1 (col_1, col_2, col_3) VALUES (1, 2.3, 'Test')");
    }

    @Test
    public void renameTable() {
        new SchemaRenameTable("table_1", "table_2").run(db);

        Collection<String> tables = getTables(db);

        assertThat(tables, not(hasItem("table_1")));
        assertThat(tables, hasItem("table_2"));
    }

    @Test
    public void dataPersistence() {
        new SchemaRenameTable("table_1", "table_2").run(db);

        try (Cursor c = db.query("table_2", null, "col_1 = ?", new String[]{"1"}, null, null, null)) {
            c.moveToFirst();

            assertEquals(1, c.getInt(c.getColumnIndex("col_1")));
            assertEquals(2.3, c.getFloat(c.getColumnIndex("col_2")), 0.01);
            assertEquals("Test", c.getString(c.getColumnIndex("col_3")));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void renameTable_emptyOldName() {
        new SchemaRenameTable("", "table_2").run(db);
    }

    @Test(expected = IllegalArgumentException.class)
    public void renameTable_emptyNewName() {
        new SchemaRenameTable("table_1", "").run(db);
    }

}
