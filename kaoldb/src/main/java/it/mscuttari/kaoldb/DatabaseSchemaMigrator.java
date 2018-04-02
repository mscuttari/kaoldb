package it.mscuttari.kaoldb;

import android.database.sqlite.SQLiteDatabase;

import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;

public abstract class DatabaseSchemaMigrator {

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new DatabaseManagementException("onUpgrade not implemented");
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new DatabaseManagementException("onDowngrade not implemented");
    }

}
