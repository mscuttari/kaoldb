package it.mscuttari.kaoldbtest.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import it.mscuttari.kaoldb.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;

public class TestDbMigrator extends DatabaseSchemaMigrator {

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
