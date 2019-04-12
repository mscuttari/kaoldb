package it.mscuttari.examples.database;

import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.interfaces.DatabaseDump;

public class FilmDbMigrator implements DatabaseSchemaMigrator {

    @Override
    public void onUpgrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {

    }

    @Override
    public void onDowngrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {

    }

}
