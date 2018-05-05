package it.mscuttari.kaoldbtest.database;

import android.database.sqlite.SQLiteDatabase;

import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

public class TestDbMigrator implements DatabaseSchemaMigrator {

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
