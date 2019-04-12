package it.mscuttari.examples.database;

import it.mscuttari.kaoldb.interfaces.DatabaseDump;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

public class GenericDbMigrator implements DatabaseSchemaMigrator {

    @Override
    public void onUpgrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {

    }

    @Override
    public void onDowngrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {

    }

}
