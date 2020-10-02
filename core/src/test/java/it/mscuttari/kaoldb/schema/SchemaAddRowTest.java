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

import it.mscuttari.kaoldb.interfaces.SchemaAction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SchemaAddRowTest extends SchemaAbstractTest {

    @Override
    protected void createDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table_1(" +
                "id INTEGER PRIMARY KEY," +
                "col_1 INTEGER NOT NULL DEFAULT '1'," +
                "col_2 TEXT)");
    }

    @Test
    public void addRow() {
        try (Cursor c = db.query("table_1", null, null, null, null, null, null)) {
            assertEquals(0, c.getCount());
        }

        SchemaAction action = new SchemaAddRow("table_1",
                new String[] {"id", "col_1", "col_2"},
                new Object[] {1, "test", null});

        action.run(db);

        try (Cursor c = db.query("table_1", null, null, null, null, null, null)) {
            assertEquals(1, c.getCount());

            c.moveToFirst();
            assertEquals(1, c.getInt(c.getColumnIndex("id")));
            assertEquals("test", c.getString(c.getColumnIndex("col_1")));
            assertNull(c.getString(c.getColumnIndex("col_2")));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTable() {
        new SchemaAddRow("", new String[]{"id", "col_1"}, new Object[] {1, "test"}).run(db);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullColumn() {
        new SchemaAddRow("", new String[]{null, "col_1"}, new Object[] {1, "test"}).run(db);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyColumn() {
        new SchemaAddRow("", new String[]{"id", ""}, new Object[] {1, "test"}).run(db);
    }

}
