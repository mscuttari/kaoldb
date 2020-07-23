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

import it.mscuttari.kaoldb.mapping.SchemaAbstractTest;

import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableColumns;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchemaDeleteColumnTest extends SchemaAbstractTest {

    @Override
    protected void createDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table_1(" +
                "id INTEGER PRIMARY KEY," +
                "col_1 INTEGER NOT NULL DEFAULT '1'," +
                "col_2 TEXT)");

        db.execSQL("INSERT INTO table_1 (id, col_1, col_2) VALUES(1, 1, 'Test1')");
        db.execSQL("INSERT INTO table_1 (id, col_1, col_2) VALUES(2, 2, NULL)");
    }

    @Test
    public void deleteColumn() {
        Collection<String> before = getTableColumns(db, "table_1");
        assertTrue(before.contains("col_1"));

        // Delete the column
        new SchemaDeleteColumn("table_1", "col_1").run(db);

        // Check that the column has been deleted
        Collection<String> after = getTableColumns(db, "table_1");
        assertFalse(after.contains("col_1"));
    }

    @Test
    public void dataPersistence() {
        new SchemaDeleteColumn("table_1", "col_1").run(db);

        try (Cursor c = db.query("table_1", null, "id = ?", new String[]{"1"}, null, null, null)) {
            c.moveToFirst();
            assertEquals(1, c.getInt(c.getColumnIndex("id")));
            assertEquals("Test1", c.getString(c.getColumnIndex("col_2")));
        }

        try (Cursor c = db.query("table_1", null, "id = ?", new String[]{"2"}, null, null, null)) {
            c.moveToFirst();
            assertEquals(2, c.getInt(c.getColumnIndex("id")));
            assertTrue(c.isNull(c.getColumnIndex("col_2")));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTable() {
        new SchemaDeleteColumn("", "col_1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyColumn() {
        new SchemaDeleteColumn("table_1", "");
    }

}
