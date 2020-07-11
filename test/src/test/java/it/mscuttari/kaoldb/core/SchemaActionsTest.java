package it.mscuttari.kaoldb.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import it.mscuttari.kaoldb.interfaces.RowDump;
import it.mscuttari.kaoldb.interfaces.TableDump;

import static it.mscuttari.kaoldb.core.SQLiteUtils.getColumnDefaultValue;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getColumnStatement;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getTableColumns;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getTablePrimaryKeys;
import static it.mscuttari.kaoldb.core.SQLiteUtils.getTables;
import static it.mscuttari.kaoldb.core.SQLiteUtils.isColumnNullable;
import static it.mscuttari.kaoldb.core.SQLiteUtils.isColumnPrimaryKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SchemaActionsTest extends AbstractTest {

    private static final String DB_NAME = "db_actions_test";
    private SQLiteDatabase db;

    /**
     * Create the database and get a writable instance.
     */
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();

        SQLiteOpenHelper dbHelper = new SQLiteOpenHelper(context, DB_NAME, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE table_0(id INTEGER PRIMARY KEY)");

                db.execSQL("CREATE TABLE table_1(" +
                        "id INTEGER PRIMARY KEY," +
                        "col_1 INTEGER NOT NULL DEFAULT '1'," +
                        "col_2 TEXT," +
                        "fk_ext INTEGER," +
                        "fk_int INTEGER," +
                        "FOREIGN KEY (fk_ext) REFERENCES table_0(id) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED," +
                        "FOREIGN KEY (fk_int) REFERENCES table_1(col_1) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)");

                db.execSQL("INSERT INTO table_1 (id, col_1, col_2, fk_ext, fk_int) VALUES(1, 1, 'Test1', NULL, NULL)");
                db.execSQL("INSERT INTO table_1 (id, col_1, col_2, fk_ext, fk_int) VALUES(2, 2, NULL, NULL, NULL)");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };

        db = dbHelper.getWritableDatabase();
        db.beginTransaction();
    }

    /**
     * Delete the database.
     */
    @After
    public void tearDown() {
        db.setTransactionSuccessful();
        db.endTransaction();

        if (db != null && db.isOpen()) {
            db.close();
        }

        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DB_NAME);
    }

    @Test
    public void getColumnStatement_valid() {
        String statement = getColumnStatement(db, "table_1", "col_1");
        assertEquals("\"col_1\" INTEGER NOT NULL DEFAULT '1'", statement);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getColumnStatement_notFound() {
        getColumnStatement(db, "table_1", "col_not_existing");
    }

    @Test
    public void isColumnNullable_yes() {
        boolean nullable = isColumnNullable(db, "table_1", "col_2");
        assertTrue(nullable);
    }

    @Test
    public void isColumnNullable_no() {
        boolean nullable = isColumnNullable(db, "table_1", "col_1");
        assertFalse(nullable);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isColumnNullable_notFound() {
        isColumnNullable(db, "table_1", "col_not_existing");
    }

    @Test
    public void getColumnDefaultValue_null() {
        String defaultValue = getColumnDefaultValue(db, "table_1", "col_2");
        assertNull(defaultValue);
    }

    @Test
    public void getColumnDefaultValue_notNull() {
        String defaultValue = getColumnDefaultValue(db, "table_1", "col_1");
        assertEquals("'1'", defaultValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getColumnDefaultValue_notFound() {
        getColumnDefaultValue(db, "table_1", "col_not_existing");
    }

    @Test
    public void isPrimaryKey_yes() {
        boolean primaryKey = isColumnPrimaryKey(db, "table_1", "id");
        assertTrue(primaryKey);
    }

    @Test
    public void isPrimaryKey_no() {
        boolean primaryKey = isColumnPrimaryKey(db, "table_1", "col_1");
        assertFalse(primaryKey);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isPrimaryKey_notFound() {
        isColumnPrimaryKey(db, "table_1", "col_not_existing");
    }

    @Test
    public void addColumn_nullable_noDefaultValue_noPk() {
        // Check that the column doesn't exist
        assertFalse(getTableColumns(db, "table_1").contains("col_add_1"));

        // Add the column
        SchemaBaseAction action = new SchemaAddColumn(
                "table_1",
                "col_add_1",
                String.class,
                true,
                null,
                false);

        action.run(db);

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

    @Test
    public void addColumn_notNullable_defaultValue_pk() {
        // Check that the column doesn't exist
        assertFalse(getTableColumns(db, "table_1").contains("col_add_1"));

        // Add the column
        Collection<String> primaryKeys = getTablePrimaryKeys(db, "table_1");

        SchemaBaseAction action = new SchemaAddColumn(
                "table_1",
                "col_add_1",
                String.class,
                false,
                "Test",
                true);

        action.run(db);

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

    @Test
    public void createTable() {
        // Check that the table doesn't exist before creation
        assertFalse(getTables(db).contains("table_new_1"));

        // Create the table
        SchemaBaseAction action = new SchemaCreateTable("table_new_1", "id", Integer.class);
        action.run(db);

        // Check that the table has been created
        assertTrue(getTables(db).contains("table_new_1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTable_emptyTable() {
        new SchemaCreateTable("", "id", Integer.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTable_emptyPrimaryKey() {
        new SchemaCreateTable("table_1", "", Integer.class);
    }

    @Test
    public void deleteTable() {
        // Check that the table exists before deleting
        assertTrue(getTables(db).contains("table_1"));

        // Delete the table
        SchemaBaseAction action = new SchemaDeleteTable("table_1");
        action.run(db);

        // Check that the table doesn't exist anymore
        assertFalse(getTables(db).contains("table_1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteTable_emptyTable() {
        new SchemaDeleteTable("");
    }

    @Test
    public void renameColumn() {
        Collection<String> before = getTableColumns(db, "table_1");
        assertTrue(before.contains("col_1"));
        assertFalse(before.contains("col_1_renamed"));

        // Rename the column
        SchemaBaseAction action = new SchemaRenameColumn("table_1", "col_1", "col_1_renamed");
        action.run(db);

        // Check that the column has been renamed
        Collection<String> after = getTableColumns(db, "table_1");
        assertFalse(after.contains("col_1"));
        assertTrue(after.contains("col_1_renamed"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void renameColumn_emptyTable() {
        new SchemaRenameColumn("", "col_1", "col_1_renamed");
    }

    @Test(expected = IllegalArgumentException.class)
    public void renameColumn_emptyOldName() {
        new SchemaRenameColumn("table_1", "", "col_1_renamed");
    }

    @Test(expected = IllegalArgumentException.class)
    public void renameColumn_emptyNewName() {
        new SchemaRenameColumn("table_1", "col_1", "");
    }

    @Test
    public void deleteColumn() {
        Collection<String> before = getTableColumns(db, "table_1");
        assertTrue(before.contains("col_1"));

        // Delete the column
        new SchemaDeleteColumn("table_1", "col_1").run(db);

        // Check that the column has been renamed
        Collection<String> after = getTableColumns(db, "table_1");
        assertFalse(after.contains("col_1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteColumn_emptyTable() {
        new SchemaDeleteColumn("", "col_1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteColumn_emptyColumn() {
        new SchemaDeleteColumn("table_1", "");
    }

    @Test
    public void renameTable() {
        SchemaBaseAction action = new SchemaRenameTable("table_1", "table_1_renamed");
        action.run(db);

        Collection<String> tables = getTables(db);

        assertFalse(tables.contains("table_1"));
        assertTrue(tables.contains("table_1_renamed"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void renameTable_emptyOldName() {
        new SchemaRenameTable("", "table_2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void renameTable_emptyNewName() {
        new SchemaRenameTable("table_1", "");
    }

}
