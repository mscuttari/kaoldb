package it.mscuttari.kaoldb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;

public final class EntityManager extends SQLiteOpenHelper {

    private DatabaseObject database;
    private Context context;


    /**
     * Constructor
     *
     * @param   context     Context             context
     * @param   database    DatabaseObject      database mapping object
     */
    EntityManager(Context context, DatabaseObject database) {
        super(context, database.name, null, database.version);
        this.database = database;
        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        for (EntityObject entity : database.entities) {
            String createSQL = TableUtils.getCreateTableSql(entity);
            if (createSQL != null) db.execSQL(createSQL);
        }
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (database.migrator == null)
            throw new DatabaseManagementException("Database " + database.name + ": schema migrator not set");

        DatabaseSchemaMigrator migrator;

        try {
            migrator = database.migrator.newInstance();
        } catch (IllegalAccessException e) {
            throw new DatabaseManagementException(e.getMessage());
        } catch (InstantiationException e) {
            throw new DatabaseManagementException(e.getMessage());
        }

        migrator.onUpgrade(db, oldVersion, newVersion);
    }


    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (database.migrator == null)
            throw new DatabaseManagementException("Database " + database.name + ": schema migrator not set");

        DatabaseSchemaMigrator migrator;

        try {
            migrator = database.migrator.newInstance();
        } catch (IllegalAccessException e) {
            throw new DatabaseManagementException(e.getMessage());
        } catch (InstantiationException e) {
            throw new DatabaseManagementException(e.getMessage());
        }

        migrator.onDowngrade(db, oldVersion, newVersion);
    }


    /**
     * Delete database
     *
     * @return true if everything went fine; false otherwise
     */
    public boolean deleteDatabase() {
        return context.deleteDatabase(database.name);
    }

}
