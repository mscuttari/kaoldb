package it.mscuttari.kaoldb.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.PostActionListener;
import it.mscuttari.kaoldb.interfaces.PreActionListener;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

/**
 * Entity manager implementation
 *
 * @see EntityManager
 */
class EntityManagerImpl implements EntityManager {

    private static Map<DatabaseObject, EntityManagerImpl> entityManagers = new HashMap<>();

    private final DatabaseObject database;
    private final WeakReference<Context> context;
    public final ConcurrentSQLiteOpenHelper dbHelper;


    /**
     * Constructor
     *
     * @param   context     context
     * @param   database    database mapping object
     */
    private EntityManagerImpl(Context context, DatabaseObject database) {
        this.database = database;
        this.context = new WeakReference<>(context);

        SQLiteOpenHelper dbHelper = new SQLiteOpenHelper(context, database.getName(), null, database.getVersion()) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                for (EntityObject entity : database.getEntities()) {
                    // Entity table
                    String entityTableCreateSQL = entity.getSQL();

                    if (entityTableCreateSQL != null) {
                        LogUtils.d("[Entity \"" + entity.getName() + "\"] Create table query: " + entityTableCreateSQL);
                        //System.out.println("[Entity \"" + entity.getName() + "\"] Create table query: " + entityTableCreateSQL);
                        db.execSQL(entityTableCreateSQL);
                        LogUtils.i("[Entity \"" + entity.getName() + "\"] Table created");
                    }

                    // Join tables
                    for (Field field : entity.relationships) {
                        if (!field.isAnnotationPresent(JoinTable.class))
                            continue;

                        JoinTableObject joinTableObject = new JoinTableObject(database, entity, field);
                        String joinTableCreateSQL = joinTableObject.getSQL();

                        if (joinTableCreateSQL != null && !joinTableCreateSQL.isEmpty()) {
                            LogUtils.d("[Entity \"" + entity.getName() + "\"] Create join table query: " + joinTableCreateSQL);
                            //System.out.println("[Entity \"" + entity.getName() + "\"] Create join table query: " + joinTableCreateSQL);
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
        };

        this.dbHelper = new ConcurrentSQLiteOpenHelper(dbHelper);
    }


    /**
     * Get singleton instance.
     * This is done to ensure that there is at most one database connection opened towards
     * each database.
     *
     * @param   context     context
     * @param   database    database object
     *
     * @return  singleton instance
     */
    public static EntityManagerImpl getEntityManager(Context context, DatabaseObject database) {
        EntityManagerImpl entityManager = entityManagers.get(database);

        if (entityManager == null) {
            entityManager = new EntityManagerImpl(context, database);
            entityManagers.put(database, entityManager);
        }

        return entityManager;
    }


    @Override
    public boolean deleteDatabase() {
        dbHelper.forceClose();
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


    @Override
    public synchronized void persist(Object obj) {
        persist(obj, null, null);
    }


    @Override
    public synchronized <T> void persist(T obj, PreActionListener<T> prePersist, PostActionListener<T> postPersist) {
        // Pre persist actions
        if (prePersist != null)
            prePersist.run(obj);

        // Current working entity and the previous child entity
        EntityObject currentEntity = database.getEntity(obj.getClass());
        EntityObject childEntity = null;

        // Open the database and start a transaction
        dbHelper.open();
        dbHelper.beginTransaction();

        try {
            while (currentEntity != null) {
                // Extract the current entity data from the object to be persisted
                ContentValues cv = PojoAdapter.objectToContentValues(getContext(), database, currentEntity, childEntity, obj);

                // Persist
                if (cv.size() != 0) {
                    dbHelper.insert(currentEntity.tableName, null, cv);
                }

                // Go up in the entity hierarchy
                childEntity = currentEntity;
                currentEntity = currentEntity.parent;
            }

            dbHelper.setTransactionSuccessful();

        } catch (Exception e) {
            throw new QueryException(e);

        } finally {
            // End the transaction and close the database
            dbHelper.endTransaction();
            dbHelper.close();
        }

        // Post persist actions
        if (postPersist != null)
            postPersist.run(obj);
    }


    @Override
    public void update(Object obj) {
        update(obj, null, null);
    }


    @Override
    public <T> void update(T obj, PreActionListener<T> preUpdate, PostActionListener<T> postUpdate) {
        // Pre update actions
        if (preUpdate != null)
            preUpdate.run(obj);

        // Current working entity and the previous child entity
        EntityObject currentEntity = database.getEntity(obj.getClass());
        EntityObject childEntity = null;

        // Open the database and start a transaction
        dbHelper.open();
        //dbHelper.beginTransaction();

        try {
            while (currentEntity != null) {
                // Extract the current entity data from the object to be persisted
                ContentValues cv = PojoAdapter.objectToContentValues(getContext(), database, currentEntity, childEntity, obj);

                // Update
                if (cv.size() != 0) {
                    dbHelper.insert(currentEntity.tableName, null, cv);

                    StringBuilder where = new StringBuilder();
                    Collection<BaseColumnObject> primaryKeys = currentEntity.columns.getPrimaryKeys();
                    List<String> whereArgs = new ArrayList<>(primaryKeys.size());

                    for (BaseColumnObject primaryKey : primaryKeys) {
                        if (where.length() > 0)
                            where.append(" AND ");

                        where.append(primaryKey.name).append(" = ?");
                        whereArgs.add(String.valueOf(primaryKey.getValue(obj)));
                    }

                    dbHelper.update(currentEntity.tableName,
                            cv,
                            where.toString(),
                            whereArgs.toArray(new String[primaryKeys.size()])
                    );
                }

                // Go up in the entity hierarchy
                childEntity = currentEntity;
                currentEntity = currentEntity.parent;
            }

            //dbHelper.setTransactionSuccessful();

        } catch (Exception e) {
            throw new QueryException(e);

        } finally {
            // End the transaction and close the database
            //dbHelper.endTransaction();
            dbHelper.close();
        }

        // Post persist actions
        if (postUpdate != null)
            postUpdate.run(obj);
    }


    @Override
    public void remove(Object obj) {
        remove(obj, null, null);
    }


    @Override
    public <T> void remove(T obj, PreActionListener<T> preRemove, PostActionListener<T> postRemove) {
        // Pre remove actions
        if (preRemove != null)
            preRemove.run(obj);

        // Current working entity and the previous child entity
        EntityObject currentEntity = database.getEntity(obj.getClass());

        // Open the database and start a transaction
        dbHelper.open();
        //dbHelper.beginTransaction();

        try {
            while (currentEntity != null) {
                // Remove
                StringBuilder where = new StringBuilder();
                Collection<BaseColumnObject> primaryKeys = currentEntity.columns.getPrimaryKeys();
                List<String> whereArgs = new ArrayList<>(primaryKeys.size());

                for (BaseColumnObject primaryKey : primaryKeys) {
                    if (where.length() > 0)
                        where.append(" AND ");

                    where.append(primaryKey.name).append(" = ?");
                    whereArgs.add(String.valueOf(primaryKey.getValue(obj)));
                }

                dbHelper.delete(currentEntity.tableName,
                        where.toString(),
                        whereArgs.toArray(new String[primaryKeys.size()])
                );

                // Go up in the entity hierarchy
                currentEntity = currentEntity.parent;
            }

            //dbHelper.setTransactionSuccessful();

        } catch (Exception e) {
            throw new QueryException(e);

        } finally {
            // End the transaction and close the database
            //dbHelper.endTransaction();
            dbHelper.close();
        }

        // Post remove actions
        if (postRemove != null)
            postRemove.run(obj);
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
