package it.mscuttari.kaoldb.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.PostPersistListener;
import it.mscuttari.kaoldb.interfaces.PrePersistListener;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static it.mscuttari.kaoldb.core.Constants.LOG_TAG;
import static it.mscuttari.kaoldb.core.PojoAdapter.cursorToObject;
import static it.mscuttari.kaoldb.core.PojoAdapter.objectToContentValues;

class EntityManagerImpl extends SQLiteOpenHelper implements EntityManager {

    private DatabaseObject database;
    private Context context;


    /**
     * Constructor
     *
     * @param   context     context
     * @param   database    database mapping object
     */
    EntityManagerImpl(Context context, DatabaseObject database) {
        super(context, database.name, null, database.version);
        this.database = database;
        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        for (EntityObject entity : database.entities.values()) {
            String createSQL = EntityUtils.getCreateTableSql(entity);
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


    /** {@inheritDoc} */
    @Override
    public boolean deleteDatabase() {
        return context.deleteDatabase(database.name);
    }


    /** {@inheritDoc} */
    @Override
    public <T> QueryBuilder<T> getQueryBuilder(Class<T> resultClass) {
        return new QueryBuilderImpl<>(database, resultClass, this);
    }


    /** {@inheritDoc} */
    @Override
    public <T> List<T> getAll(Class<T> entityClass) {
        QueryBuilder<T> qb = getQueryBuilder(entityClass);
        Root<T> root = qb.getRoot(entityClass, "c");
        qb.from(root);

        return qb.build("e").getResultList();
    }


    /**
     * Get the entity of an object
     *
     * @param   obj     object
     * @return  entity
     * @throws  QueryException if the object is not an entity
     */
    private EntityObject getObjectEntity(Object obj) {
        Class<?> objectClass = obj.getClass();
        EntityObject entity = database.entities.get(objectClass);

        if (entity == null)
            throw new QueryException("Class " + objectClass.getSimpleName() + " is not an entity");

        return entity;
    }


    /**
     * Get the primary keys values at a specific entity level
     *
     * @param   entity      entity
     * @param   obj         object
     *
     * @return  primary keys
     */
    private static Map<String, Object> getPrimaryKeys(EntityObject entity, Object obj) {
        if (!entity.realTable)
            return null;

        Map<String, Object> result = new HashMap<>(entity.primaryKeys.size());

        for (ColumnObject column : entity.primaryKeys) {
            try {
                Object value = column.field.get(obj);
                result.put(column.name, value);
            } catch (IllegalAccessException e) {
                throw new QueryException(e.getMessage());
            }
        }

        return result;
    }


    /** {@inheritDoc} */
    @Override
    public synchronized void persist(Object obj) {
        persist(obj, null, null);
    }


    /** {@inheritDoc} */
    @Override
    public synchronized <M> void persist(M obj, PrePersistListener<M> prePersist, PostPersistListener<M> postPersist) {
        if (prePersist != null) prePersist.prePersist(obj);
        EntityObject entity = getObjectEntity(obj);
        persist(obj, entity, null, false);
        if (postPersist != null) postPersist.postPersist(obj);
    }


    /**
     * Persist object in the database
     *
     * @param   obj                 object to be persisted
     * @param   currentEntity       current entity
     * @param   childEntity         child entity (used to determine the discriminator value)
     * @param   isInTransaction     true if the transaction is already started, false if the first iteration
     */
    private synchronized void persist(Object obj, EntityObject currentEntity, EntityObject childEntity, boolean isInTransaction) {
        // Extract the current entity data from the object to be persisted
        ContentValues cv = objectToContentValues(context, database, currentEntity, childEntity, obj);

        // Do the same for its parent
        if (currentEntity.parent != null)
            persist(obj, currentEntity.parent, currentEntity, true);

        // Persist the object
        SQLiteDatabase db = getWritableDatabase();

        try {
            if (!isInTransaction) db.beginTransaction();
            if (cv.size() != 0) db.insert(currentEntity.tableName, null, cv);

        } finally {
            if (!isInTransaction) {
                // End the transaction and close the database
                db.setTransactionSuccessful();
                db.endTransaction();
                if (db.isOpen()) db.close();
            }
        }
    }


    /** {@inheritDoc} */
    public void postPersist(Object obj) {

    }


    /** {@inheritDoc} */
    public synchronized void delete(Object obj, boolean isInTransaction) {
        EntityObject entity = getObjectEntity(obj);
        SQLiteDatabase db = getWritableDatabase();
        if (!isInTransaction) db.beginTransaction();

        try {
            while (entity != null) {
                Map<String, Object> primaryKeys = getPrimaryKeys(entity, obj);

                if (primaryKeys != null) {
                    StringBuilder whereClause = new StringBuilder();
                    String separator = "";
                    String[] whereArgs = new String[primaryKeys.keySet().size()];
                    int counter = 0;

                    for (String key : primaryKeys.keySet()) {
                        whereClause.append(separator).append(key);
                        separator = " AND ";
                        Object value = primaryKeys.get(key);

                        if (value == null) {
                            whereClause.append(" IS NULL");
                        } else {
                            whereClause.append("=?");
                            whereArgs[counter++] = String.valueOf(value);
                        }
                    }

                    db.delete(entity.tableName, whereClause.toString(), whereArgs);
                }

                entity = entity.parent;
            }
        } finally {
            if (!isInTransaction) db.endTransaction();
            db.close();
        }
    }

}
