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

package it.mscuttari.kaoldb.core;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;

final class ConcurrentSQLiteOpenHelper {

    private final SQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;
    private int dbConnections = 0;


    /**
     * Constructor.
     *
     * @param dbHelper      database helper that needs thread safety
     */
    public ConcurrentSQLiteOpenHelper(SQLiteOpenHelper dbHelper) {
        this.dbHelper = dbHelper;
    }


    /**
     * Open the database.
     */
    public synchronized void open() {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }

        dbConnections++;
    }


    /**
     * Close the database.
     */
    public synchronized void close() {
        if (--dbConnections == 0) {
            db.close();
            db = null;
        }
    }


    /**
     * Close the database independently from its current usage.
     */
    public synchronized void forceClose() {
        if (db != null) {
            if (db.isOpen()) {
                if (db.inTransaction())
                    endTransaction();

                db.close();
            }
        }

        dbConnections = 0;
        db = null;
    }


    /**
     * Begin transaction.
     *
     * @throws DatabaseManagementException if the database has not been opened yet
     * @throws DatabaseManagementException if a transaction is already running
     */
    public synchronized void beginTransaction() {
        if (db == null)
            throw new DatabaseManagementException("Database must be opened first");

        if (db.inTransaction())
            throw new DatabaseManagementException("A transaction is already running");

        db.beginTransaction();
    }


    /**
     * Set the current transaction as successful.
     *
     * @throws DatabaseManagementException if the database has not been opened yet
     * @throws DatabaseManagementException if there is no transaction running
     */
    public synchronized void setTransactionSuccessful() {
        if (db == null)
            throw new DatabaseManagementException("Database must be opened first");

        if (!db.inTransaction())
            throw new DatabaseManagementException("There is no transaction running");

        db.setTransactionSuccessful();
    }


    /**
     * End the current transaction.
     *
     * @throws DatabaseManagementException if the database has not been opened yet
     * @throws DatabaseManagementException if there is no transaction running
     */
    public synchronized void endTransaction() {
        if (db == null)
            throw new DatabaseManagementException("Database must be opened first");

        if (!db.inTransaction())
            throw new DatabaseManagementException("There is no transaction running");

        db.endTransaction();
    }


    /**
     * Perform a SELECT query.
     *
     * @param sql               query
     * @param selectionArgs     selection args
     *
     * @return cursor containing the data
     */
    public synchronized Cursor select(String sql, String[] selectionArgs) {
        if (db == null)
            throw new DatabaseManagementException("Database must be opened first");

        return new CachedCursor(db.rawQuery(sql, selectionArgs));
    }


    /**
     * Perform an insertion into the database.
     *
     * @param table             table name
     * @param nullColumnHack    see {@link SQLiteDatabase#insert(String, String, ContentValues)}
     * @param values            data to be inserted
     *
     * @return row ID
     */
    public synchronized long insert(String table, String nullColumnHack, ContentValues values) {
        boolean shortRun = db == null;

        if (shortRun)
            open();

        try {
            return db.insert(table, nullColumnHack, values);
        } finally {
            if (shortRun)
                close();
        }
    }


    /**
     * Perform an update on the existing data matching the selection clause.
     *
     * @param table         table name
     * @param values        new data
     * @param whereClause   selection clause
     * @param whereArgs     selection args
     *
     * @return number of rows affected
     */
    public synchronized int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        boolean shortRun = db == null;

        if (shortRun)
            open();

        try {
            return db.update(table, values, whereClause, whereArgs);
        } finally {
            if (shortRun)
                close();
        }
    }


    /**
     * Delete the entries matching the selection clause.
     *
     * @param table         table name
     * @param whereClause   selection clause
     * @param whereArgs     selection args
     *
     * @return number of rows affected
     */
    public synchronized int delete(String table, String whereClause, String[] whereArgs) {
        boolean shortRun = db == null;

        if (shortRun)
            open();

        try {
            return db.delete(table, whereClause, whereArgs);
        } finally {
            if (shortRun)
                close();
        }
    }

}
