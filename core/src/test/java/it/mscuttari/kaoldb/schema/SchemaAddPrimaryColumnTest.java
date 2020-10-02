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

import java.util.List;

import it.mscuttari.kaoldb.dump.TableDumpImpl;
import it.mscuttari.kaoldb.interfaces.RowDump;
import it.mscuttari.kaoldb.interfaces.TableDump;

import static it.mscuttari.kaoldb.dump.SQLiteUtils.getTableColumns;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SchemaAddPrimaryColumnTest extends SchemaAbstractTest {

    @Override
    protected void createDb(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE table_1(" +
                "id INTEGER PRIMARY KEY," +
                "col_1 INTEGER NOT NULL DEFAULT '1'," +
                "col_2 TEXT)");

        db.execSQL("INSERT INTO table_1 (id, col_1, col_2) VALUES (1, 1, 'Test')");
        db.execSQL("INSERT INTO table_1 (id, col_1, col_2) VALUES (2, 2, NULL)");
    }

    @Test
    public void add() {
        // Check that the column doesn't exist
        assertTrue(getTableColumns(db, "table_1").stream().noneMatch(c -> c.name.equals("col_add_1")));

        Column column = new Column("col_add_1", String.class, null, true, true, false);
        new SchemaAddColumn("table_1", column).run(db);

        // Check that the column now exists
        assertTrue(getTableColumns(db, "table_1").stream().anyMatch(c -> c.name.equals("col_add_1")));
    }

    @Test
    public void dataPersistence() {
        Column column = new Column("col_add_1", String.class, null, true, true, false);
        new SchemaAddColumn("table_1", column).run(db);

        TableDump tableDump = new TableDumpImpl(db, "table_1");
        List<RowDump> tableRows = tableDump.getRows();

        assertEquals(1, (int) tableRows.get(0).getColumnValue("id"));
        assertEquals(1, (int) tableRows.get(0).getColumnValue("col_1"));
        assertEquals("Test", tableRows.get(0).getColumnValue("col_2"));

        assertEquals(2, (int) tableRows.get(1).getColumnValue("id"));
        assertEquals(2, (int) tableRows.get(1).getColumnValue("col_1"));
        assertNull(tableRows.get(1).getColumnValue("col_2"));
    }

    @Test
    public void defaultValue() {
        Column column = new Column("col_add_1", String.class, "Default", true, false, false);
        new SchemaAddColumn("table_1", column).run(db);

        TableDump tableDump = new TableDumpImpl(db, "table_1");
        List<RowDump> tableRows = tableDump.getRows();

        // Check that the already existing rows get the default value in the new column
        assertEquals("Default", (String) tableRows.get(0).getColumnValue("col_add_1"));
        assertEquals("Default", (String) tableRows.get(1).getColumnValue("col_add_1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTable() {
        Column column = new Column("col_add_1", String.class, null, true, false, false);
        new SchemaAddColumn("", column).run(db);
    }

}
