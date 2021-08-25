package it.mscuttari.kaoldb.examples.generic.migrators;

import java.util.List;

import it.mscuttari.kaoldb.interfaces.DatabaseDump;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.interfaces.SchemaAction;

public class Migrator implements DatabaseSchemaMigrator {

    @Override
    public List<SchemaAction> onUpgrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {
        return null;
    }

    @Override
    public List<SchemaAction> onDowngrade(int oldVersion, int newVersion, DatabaseDump dump) throws Exception {
        return null;
    }

}