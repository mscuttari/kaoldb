package it.mscuttari.kaoldb.interfaces;

import android.database.sqlite.SQLiteDatabase;

/**
 * Database schema migrator to be used in case of database version change
 */
public interface DatabaseSchemaMigrator {

    /**
     * Called in case of database version upgrade
     *
     * @param db            SQLite database
     * @param oldVersion    old version
     * @param newVersion    new version
     */
    void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);


    /**
     * Called in case of database version downgrade
     *
     * @param db            SQLite database
     * @param oldVersion    old version
     * @param newVersion    new version
     */
    void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion);

}
