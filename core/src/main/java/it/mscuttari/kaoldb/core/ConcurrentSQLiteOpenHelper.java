package it.mscuttari.kaoldb.core;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;

final class ConcurrentSQLiteOpenHelper {

    private final SQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;
    private final AtomicInteger dbConnections = new AtomicInteger(0);


    /**
     * Constructor
     *
     * @param   dbHelper    database helper that needs thread safety
     */
    public ConcurrentSQLiteOpenHelper(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }


    /**
     * Open the database
     */
    public synchronized void open() {
        if (db == null)
            db = dbHelper.getWritableDatabase();

        dbConnections.incrementAndGet();
    }


    /**
     * Close the database
     */
    public synchronized void close() {
        if (dbConnections.decrementAndGet() == 0 && db != null) {
            if (db.isOpen())
                db.close();

            db = null;
        }
    }


    /**
     * Close the database independently from its current usage
     */
    public synchronized void forceClose() {
        if (db != null) {
            if (db.isOpen()) {
                if (db.inTransaction())
                    endTransaction();

                db.close();
            }
        }

        dbConnections.set(0);
        db = null;
    }


    /**
     * Begin transaction
     *
     * @throws  DatabaseManagementException if the database has not been opened yet
     * @throws  DatabaseManagementException if a transaction is already running
     */
    public synchronized void beginTransaction() {
        if (db == null)
            throw new DatabaseManagementException("Database must be opened first");

        if (db.inTransaction())
            throw new DatabaseManagementException("A transaction is already running");

        db.beginTransaction();
    }


    /**
     * Set the current transaction as successful
     *
     * @throws  DatabaseManagementException if the database has not been opened yet
     * @throws  DatabaseManagementException if there is no transaction running
     */
    public synchronized void setTransactionSuccessful() {
        if (db == null)
            throw new DatabaseManagementException("Database must be opened first");

        if (!db.inTransaction())
            throw new DatabaseManagementException("There is no transaction running");

        db.setTransactionSuccessful();
    }


    /**
     * End the current transaction
     *
     * @throws  DatabaseManagementException if the database has not been opened yet
     * @throws  DatabaseManagementException if there is no transaction running
     */
    public synchronized void endTransaction() {
        if (db == null)
            throw new DatabaseManagementException("Database must be opened first");

        if (!db.inTransaction())
            throw new DatabaseManagementException("There is no transaction running");

        db.endTransaction();
    }


    /**
     * Perform a SELECT query
     *
     * @param   sql             query
     * @param   selectionArgs   selection args
     *
     * @return  cursor containing the data
     */
    public synchronized Cursor select(String sql, String[] selectionArgs) {
        if (db == null)
            throw new DatabaseManagementException("Database must be opened first");

        return db.rawQuery(sql, selectionArgs);
    }


    /**
     * Perform an insertion into the database
     *
     * @param   table           table name
     * @param   nullColumnHack  see {@link SQLiteDatabase#insert(String, String, ContentValues)}
     * @param   values          data to be inserted
     *
     * @return  row ID
     */
    public synchronized long insert(String table, String nullColumnHack, ContentValues values) {
        boolean shortRun = db == null;
        if (shortRun) open();

        try {
            return db.insert(table, nullColumnHack, values);
        } finally {
            if (shortRun) close();
        }
    }


    /**
     * Perform an update of some already existing data
     *
     * @param   table           table name
     * @param   values          new data
     * @param   whereClause     selection clause
     * @param   whereArgs       selection args
     *
     * @return  number of rows affected
     */
    public synchronized int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        boolean shortRun = db == null;
        if (shortRun) open();

        try {
            return db.update(table, values, whereClause, whereArgs);
        } finally {
            if (shortRun) close();
        }
    }


    /**
     * Delete some entries
     *
     * @param   table           table name
     * @param   whereClause     selection clause
     * @param   whereArgs       selection args
     *
     * @return  number of rows affected
     */
    public synchronized int delete(String table, String whereClause, String[] whereArgs) {
        boolean shortRun = db == null;
        if (shortRun) open();

        try {
            return db.delete(table, whereClause, whereArgs);
        } finally {
            if (shortRun) close();
        }
    }

}
