package it.mscuttari.kaoldb.schema;

import android.database.sqlite.SQLiteDatabase;

public final class SchemaActionRunner {

    private final SchemaBaseAction action;

    public SchemaActionRunner(SchemaBaseAction action) {
        this.action = action;
    }

    public void run(SQLiteDatabase db) {
        action.run(db);
    }

}
