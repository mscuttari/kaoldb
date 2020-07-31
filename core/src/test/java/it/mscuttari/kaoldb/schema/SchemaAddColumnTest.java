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

import java.util.Collection;

import static it.mscuttari.kaoldb.dump.SQLiteUtils.getColumnDefaultValue;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTablePrimaryKeys;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.isColumnNullable;
import static it.mscuttari.kaoldb.dump.SQLiteUtils.isColumnPrimaryKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// TODO: check all
public class SchemaAddColumnTest extends SchemaAbstractTest {

    @Override
    protected void createDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table_1(" +
                "id INTEGER PRIMARY KEY," +
                "col_1 INTEGER NOT NULL DEFAULT '1'," +
                "col_2 TEXT)");
    }

    // TODO
    /*
    @Test
    public void addColumn_nullable_noDefaultValue_noPk() {
        // Check that the column doesn't exist
        assertFalse(getTableColumns(db, "table_1").contains("col_add_1"));

        // Add the column
        new SchemaAddColumn("table_1", "col_add_1", String.class, true, null, false).run(db);

        // Check column existence
        assertTrue(getTableColumns(db, "table_1").contains("col_add_1"));
        assertTrue(isColumnNullable(db, "table_1", "col_add_1"));
        assertNull(getColumnDefaultValue(db, "table_1", "col_add_1"));
        assertFalse(isColumnPrimaryKey(db, "table_1", "col_add_1"));

        // Check that data is not lost
        TableDump tableDump = new TableDumpImpl(db, "table_1");
        List<RowDump> tableRows = tableDump.getRows();

        assertEquals(1, (int) tableRows.get(0).getColumnValue("id"));
        assertEquals(1, (int) tableRows.get(0).getColumnValue("col_1"));
        assertEquals("Test1", tableRows.get(0).getColumnValue("col_2"));

        assertEquals(2, (int) tableRows.get(1).getColumnValue("id"));
        assertEquals(2, (int) tableRows.get(1).getColumnValue("col_1"));
        assertNull(tableRows.get(1).getColumnValue("col_2"));
    }
    */

    @Test
    public void addColumn_notNullable_defaultValue_pk() {
        // Check that the column doesn't exist
        assertFalse(getTableColumns(db, "table_1").contains("col_add_1"));

        // Add the column
        Collection<String> primaryKeys = getTablePrimaryKeys(db, "table_1");

        new SchemaAddColumn("table_1", "col_add_1", String.class, false, "Test", true).run(db);

        // Check column existence
        assertTrue(getTableColumns(db, "table_1").contains("col_add_1"));
        assertFalse(isColumnNullable(db, "table_1", "col_add_1"));
        assertEquals("\"Test\"", getColumnDefaultValue(db, "table_1", "col_add_1"));
        assertTrue(isColumnPrimaryKey(db, "table_1", "col_add_1"));

        // Also check that the previous primary keys has not been lost
        assertTrue(getTablePrimaryKeys(db, "table_1").containsAll(primaryKeys));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addColumn_emptyTable() {
        new SchemaAddColumn("", "col_add_1", Integer.class, true, null, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addColumn_emptyColumn() {
        new SchemaAddColumn("table_1", "", Integer.class, true, null, false);
    }

}
