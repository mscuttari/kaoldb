package it.mscuttari.kaoldb.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.PostPersistListener;
import it.mscuttari.kaoldb.interfaces.PrePersistListener;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static it.mscuttari.kaoldb.core.PojoAdapter.objectToContentValues;

/**
 * Entity manager implementation
 *
 * @see EntityManager
 */
class EntityManagerImpl extends SQLiteOpenHelper implements EntityManager {

    private final DatabaseObject database;
    private WeakReference<Context> context;


    /**
     * Constructor
     *
     * @param   context     context
     * @param   database    database mapping object
     */
    EntityManagerImpl(Context context, DatabaseObject database) {
        super(context, database.getName(), null, database.getVersion());

        this.database = database;
        this.context = new WeakReference<>(context);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        for (EntityObject entity : database.getEntities()) {
            // Entity table
            String entityTableCreateSQL = EntityUtils.getTableSql(database, entity);

            if (entityTableCreateSQL != null) {
                LogUtils.d("[Entity \"" + entity.getName() + "\"] Create table query: " + entityTableCreateSQL);
                System.out.println("[Entity \"" + entity.getName() + "\"] Create table query: " + entityTableCreateSQL);
                db.execSQL(entityTableCreateSQL);
                LogUtils.i("[Entity \"" + entity.getName() + "\"] Table created");
            }

            // Join tables
            for (Field field : entity.relationships) {
                String joinTableCreateSQL = EntityUtils.getJoinTableSql(database, field);

                if (joinTableCreateSQL != null) {
                    LogUtils.d("[Entity \"" + entity.getName() + "\"] Create join table query: " + joinTableCreateSQL);
                    System.out.println("[Entity \"" + entity.getName() + "\"] Create join table query: " + joinTableCreateSQL);
                    db.execSQL(joinTableCreateSQL);
                    LogUtils.i("[Entity \"" + entity.getName() + "\"] Join table created");
                }
            }
        }
    }


    /**
     * Called when the database needs to be upgraded
     *
     * @param   db          database.
     * @param   oldVersion  old database version
     * @param   newVersion  new database version
     *
     * @throws DatabaseManagementException if the database schema migrator can't be instantiated
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.d("[Database \"" + database.getName() + "\"] upgrading from version " + oldVersion + " to version " + newVersion);

        try {
            DatabaseSchemaMigrator migrator = database.getSchemaMigrator().newInstance();
            migrator.onUpgrade(db, oldVersion, newVersion);

        } catch (IllegalAccessException e) {
            throw new DatabaseManagementException(e);

        } catch (InstantiationException e) {
            throw new DatabaseManagementException(e);
        }

        LogUtils.i("[Database \"" + database.getName() + "\"]: upgraded from version " + oldVersion + " to version " + newVersion);
    }


    /**
     * Called when the database needs to be downgraded
     *
     * @param   db          database.
     * @param   oldVersion  old database version
     * @param   newVersion  new database version
     *
     * @throws DatabaseManagementException if the database schema migrator can't be instantiated
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.d("[Database \"" + database.getName() + "\"] upgrading from version " + oldVersion + " to version " + newVersion);

        try {
            DatabaseSchemaMigrator migrator = database.getSchemaMigrator().newInstance();
            migrator.onDowngrade(db, oldVersion, newVersion);

        } catch (IllegalAccessException e) {
            throw new DatabaseManagementException(e);

        } catch (InstantiationException e) {
            throw new DatabaseManagementException(e);
        }

        LogUtils.i("[Database \"" + database.getName() + "\"]: downgraded from version " + oldVersion + " to version " + newVersion);
    }


    @Override
    public boolean deleteDatabase() {
        boolean result = getContext().deleteDatabase(database.getName());

        if (result) {
            LogUtils.i("Database \"" + database.getName() + "\" deleted");
        }

        return result;
    }


    @Override
    public <T> QueryBuilder<T> getQueryBuilder(Class<T> resultClass) {
        return new QueryBuilderImpl<>(database, resultClass, this);
    }


    @Override
    public <T> List<T> getAll(Class<T> entityClass) {
        QueryBuilder<T> qb = getQueryBuilder(entityClass);
        Root<T> root = qb.getRoot(entityClass, "e");
        qb.from(root);

        return qb.build("e").getResultList();
    }


    /**
     * Get the {@link EntityObject} of an object
     *
     * @param   obj     object
     * @return  entity object
     */
    private EntityObject getObjectEntity(Object obj) {
        Class<?> objectClass = obj.getClass();
        return database.getEntity(objectClass);
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

        for (ColumnObject primaryKey : entity.primaryKeys) {
            Object value = primaryKey.getValue(obj);
            result.put(primaryKey.name, value);
        }

        return result;
    }


    @Override
    public synchronized void persist(Object obj) {
        persist(obj, null, null);
    }


    @Override
    public synchronized <T> void persist(T obj, PrePersistListener<T> prePersist, PostPersistListener<T> postPersist) {
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
        ContentValues cv = objectToContentValues(getContext(), database, currentEntity, childEntity, obj);

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


    /**
     * Get {@link Context}
     *
     * @return  context
     */
    private Context getContext() {
        Context context = this.context.get();

        if (context == null)
            throw new KaolDBException("Context is null");

        return context;
    }

}
