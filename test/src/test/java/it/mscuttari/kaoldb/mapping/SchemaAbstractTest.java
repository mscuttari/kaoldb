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

package it.mscuttari.kaoldb.mapping;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;

import it.mscuttari.kaoldb.AbstractTest;

import static org.hamcrest.Matchers.hasItem;

public abstract class SchemaAbstractTest extends AbstractTest {

    protected static final String DB_NAME = "db_actions_test";
    protected SQLiteDatabase db;

    /**
     * Called when the database has to be created and populated.
     *
     * @param db    writable database
     */
    protected abstract void createDb(SQLiteDatabase db);

    /**
     * Create the database and get a writable instance.
     */
    @Before
    public final void setUp() {
        Context context = ApplicationProvider.getApplicationContext();

        SQLiteOpenHelper dbHelper = new SQLiteOpenHelper(context, DB_NAME, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.beginTransaction();
                createDb(db);
                db.setTransactionSuccessful();
                db.endTransaction();
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
    public final void tearDown() {
        db.setTransactionSuccessful();
        db.endTransaction();

        if (db != null && db.isOpen()) {
            db.close();
        }

        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DB_NAME);
    }

}
