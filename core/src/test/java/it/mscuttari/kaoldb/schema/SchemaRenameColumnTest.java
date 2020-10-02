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
import java.util.stream.Collectors;

import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableForeignKeys;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchemaRenameColumnTest extends SchemaAbstractTest {

    @Override
    protected void createDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table_0 (id INTEGER PRIMARY KEY)");

        db.execSQL("CREATE TABLE table_1 (" +
                "id INTEGER PRIMARY KEY," +
                "col_1 INTEGER NOT NULL DEFAULT '1'," +
                "col_2 TEXT," +
                "fk_ext INTEGER," +
                "fk_int TEXT," +
                "FOREIGN KEY (fk_ext) REFERENCES table_0(id) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED," +
                "FOREIGN KEY (fk_int) REFERENCES table_1(col_2) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)");

        db.execSQL("INSERT INTO table_0 (id) VALUES(0)");
        db.execSQL("INSERT INTO table_1 (id, col_1, col_2, fk_ext, fk_int) VALUES(1, 1, 'Test1', 0, NULL)");
        db.execSQL("INSERT INTO table_1 (id, col_1, col_2, fk_ext, fk_int) VALUES(2, 2, NULL, NULL, 1)");
    }

    @Test
    public void renameColumn() {
        Collection<String> before = getTableColumns(db, "table_1").stream().map(column -> column.name).collect(Collectors.toList());
        assertTrue(before.contains("col_1"));
        assertFalse(before.contains("col_1_renamed"));

        // Rename the column
        new SchemaRenameColumn("table_1", "col_1", "col_1_renamed").run(db);

        // Check that the column has been renamed
        Collection<String> after = getTableColumns(db, "table_1").stream().map(column -> column.name).collect(Collectors.toList());
        assertFalse(after.contains("col_1"));
        assertTrue(after.contains("col_1_renamed"));
    }

    @Test
    public void dataPersistence() {
        new SchemaRenameColumn("table_1", "col_1", "col_1_renamed").run(db);

        try (Cursor c = db.query("table_1", null, "id = ?", new String[]{"1"}, null, null, null)) {
            c.moveToFirst();
            assertEquals(1, c.getInt(c.getColumnIndex("id")));
            assertEquals(1, c.getInt(c.getColumnIndex("col_1_renamed")));
            assertEquals("Test1", c.getString(c.getColumnIndex("col_2")));
            assertEquals(0, c.getInt(c.getColumnIndex("fk_ext")));
            assertTrue(c.isNull(c.getColumnIndex("fk_int")));
        }

        try (Cursor c = db.query("table_1", null, "id = ?", new String[]{"2"}, null, null, null)) {
            c.moveToFirst();
            assertEquals(2, c.getInt(c.getColumnIndex("id")));
            assertEquals(2, c.getInt(c.getColumnIndex("col_1_renamed")));
            assertTrue(c.isNull(c.getColumnIndex("col_2")));
            assertTrue(c.isNull(c.getColumnIndex("fk_ext")));
            assertEquals(1, c.getInt(c.getColumnIndex("fk_int")));
        }
    }

    @Test
    public void renameExternallyReferencedColumn() {
        // TODO: rename id in table_0 and have table_1 automatically adjusted
    }

    @Test
    public void renameInternallyReferencedColumn() {
        new SchemaRenameColumn("table_1", "col_2", "col_2_renamed").run(db);
        Collection<ForeignKey> constraints = getTableForeignKeys(db, "table_1");
        assertThat(constraints, hasItem(new ForeignKey("table_1", "fk_int", "table_1", "col_2_renamed", "CASCADE", "CASCADE")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTable() {
        new SchemaRenameColumn("", "col_1", "col_1_renamed").run(db);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyOldName() {
        new SchemaRenameColumn("table_1", "", "col_1_renamed").run(db);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyNewName() {
        new SchemaRenameColumn("table_1", "col_1", "").run(db);
    }

}
