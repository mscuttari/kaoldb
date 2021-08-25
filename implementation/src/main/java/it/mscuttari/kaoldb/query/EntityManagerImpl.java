/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.query;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.lifecycle.LiveData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import it.mscuttari.kaoldb.LogUtils;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.PostActionListener;
import it.mscuttari.kaoldb.interfaces.PreActionListener;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldb.mapping.BaseColumnObject;
import it.mscuttari.kaoldb.mapping.DatabaseObject;
import it.mscuttari.kaoldb.mapping.EntityObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Entity manager implementation.
 *
 * @see EntityManager
 */
public class EntityManagerImpl implements EntityManager {

    /** Unique entity manager for each database */
    private static Map<DatabaseObject, EntityManagerImpl> entityManagers = new ArrayMap<>();

    /** Android application context */
    private final WeakReference<Context> context;

    /** The database managed by this entity manager */
    @NonNull private final DatabaseObject database;

    public final ConcurrentSQLiteOpenHelper dbHelper;

    /** Map between the observed entities and the queries to be executed when they are modified */
    private final Map<EntityObject<?>, Collection<WeakReference<LiveQuery<?>>>> observers = new ArrayMap<>();

    /**
     * Constructor.
     *
     * @param context  application context
     * @param database database
     */
    private EntityManagerImpl(@NonNull Context context, @NonNull DatabaseObject database) {
        this.context = new WeakReference<>(checkNotNull(context));
        this.database = database;
        this.dbHelper = new ConcurrentSQLiteOpenHelper(context, database);
    }

    /**
     * Get singleton instance.
     *
     * <p>
     * The decision to use a singleton has been taken to ensure that there is at most one
     * database connection opened towards each database.
     * </p>
     *
     * @param context  context
     * @param database database object
     * @return singleton instance
     */
    public static EntityManagerImpl getEntityManager(@NonNull Context context, @NonNull DatabaseObject database) {
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
            LogUtils.i("[Database \"" + database.getName() + "\"] database deleted");
        } else {
            LogUtils.e("[Database \"" + database.getName() + "\"] can't delete the database");
        }

        return result;
    }

    @NonNull
    @Override
    public <T> QueryBuilder<T> getQueryBuilder(@NonNull Class<T> resultClass) {
        database.waitUntilReady();
        return new QueryBuilderImpl<>(database, resultClass, this);
    }

    @NonNull
    @Override
    public <T> List<T> getAll(@NonNull Class<T> entityClass) {
        database.waitUntilReady();

        QueryBuilder<T> qb = getQueryBuilder(entityClass);
        Root<T> root = qb.getRoot(entityClass);
        qb.from(root);

        return qb.build(root).getResults();
    }

    @NonNull
    @Override
    public <T> LiveData<List<T>> getAllLive(@NonNull Class<T> entityClass) {
        database.waitUntilReady();

        QueryBuilder<T> qb = getQueryBuilder(entityClass);
        Root<T> root = qb.getRoot(entityClass);
        qb.from(root);

        return qb.build(root).getLiveResults();
    }

    @Override
    public synchronized void persist(Object obj) {
        persist(obj, null, null);
    }

    @Override
    public synchronized <T> void persist(T obj, PreActionListener<T> prePersist, PostActionListener<T> postPersist) {
        database.waitUntilReady();

        // Pre persist actions
        if (prePersist != null) {
            prePersist.run(obj);
        }

        // Keep track of observers that should be notified
        Collection<WeakReference<LiveQuery<?>>> touchedObservers = new ArraySet<>();

        // Current working entity and the previous child entity
        EntityObject<?> currentEntity = database.getEntity(obj.getClass());

        // Open the database and start a transaction
        dbHelper.open();
        dbHelper.beginTransaction();

        try {
            Stack<Pair<EntityObject<?>, ContentValues>> data = new Stack<>();

            // When the primary key is not set and left to the autoincrement, then the subclasses
            // need to wait for their parent to be persisted and then copy their inherited primary
            // keys. Otherwise, the autoincrement key of the children can potentially be different.
            // This forces us to persist the data starting for the top of the hierarchy tree.

            while (currentEntity != null) {
                // Save the observers for this entity
                Collection<WeakReference<LiveQuery<?>>> obs = observers.get(currentEntity);

                if (obs != null) {
                    touchedObservers.addAll(obs);
                }

                // Extract the current entity data from the object to be persisted
                ContentValues cv = currentEntity.toContentValues(obj, this);

                // Save the data for later, when we will insert it starting from the parent entity
                data.push(new Pair<>(currentEntity, cv));

                // Go up in the entity hierarchy
                currentEntity = currentEntity.getParent();
            }

            ContentValues primaryKeys = new ContentValues();

            while (!data.empty()) {
                Pair<EntityObject<?>, ContentValues> current = data.pop();

                EntityObject<?> entity = current.first;
                ContentValues cv = current.second;

                // Inherit the primary keys of the parent entities. Overwriting is safe, as
                // inherited primary key values should be equal to the parent ones by design.
                cv.putAll(primaryKeys);

                // Persist the current entity
                LogUtils.d("[Database \"" + database.getName() + "\"] insert into " + entity.tableName + ": " + cv.toString());
                long rowId = dbHelper.insert(entity.tableName, null, cv);

                Cursor c = dbHelper.select("SELECT * FROM " + entity.tableName + " WHERE rowid = ?", new String[]{ String.valueOf(rowId) });
                c.moveToFirst();

                for (BaseColumnObject primaryKey : entity.columns.getPrimaryKeys()) {
                    int columnIndex = c.getColumnIndex(primaryKey.name);

                    if (c.getType(columnIndex) == Cursor.FIELD_TYPE_BLOB) {
                        primaryKeys.put(primaryKey.name, c.getBlob(columnIndex));
                    } else {
                        primaryKeys.put(primaryKey.name, c.getString(columnIndex));
                    }
                }

                c.close();
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
        if (postPersist != null) {
            postPersist.run(obj);
        }

        // Notify the observers
        for (WeakReference<LiveQuery<?>> observer : touchedObservers) {
            LiveQuery<?> query = observer.get();

            if (query != null) {
                query.refresh();
            }
        }
    }

    @Override
    public void update(Object obj) {
        update(obj, null, null);
    }

    @Override
    public <T> void update(T obj, PreActionListener<T> preUpdate, PostActionListener<T> postUpdate) {
        database.waitUntilReady();

        // Pre update actions
        if (preUpdate != null) {
            preUpdate.run(obj);
        }

        // Keep track of observers that should be notified
        Collection<WeakReference<LiveQuery<?>>> touchedObservers = new ArraySet<>();

        // Current working entity and the previous child entity
        EntityObject<?> currentEntity = database.getEntity(obj.getClass());

        // Open the database and start a transaction
        dbHelper.open();
        dbHelper.beginTransaction();

        try {
            while (currentEntity != null) {
                // Save the observers for this entity
                Collection<WeakReference<LiveQuery<?>>> obs = observers.get(currentEntity);

                if (obs != null) {
                    touchedObservers.addAll(obs);
                }

                // If the subclass is changed, we need to remove the entries of the old subclass tables
                boolean isSameChild = true;

                if (currentEntity.getParent() != null) {
                    EntityObject<?> parent = currentEntity.getParent();

                    for (EntityObject<?> child : parent.children) {
                        Pair<String, String[]> where = getWhereFilter(parent.columns.getPrimaryKeys(), obj);

                        if (child.equals(currentEntity)) {
                            try (Cursor c = dbHelper.select("SELECT * FROM " + child.tableName + " WHERE " + where.first, where.second)) {
                                isSameChild = c.getCount() > 0;
                            }
                        } else {
                            LogUtils.d("[Database \"" + database.getName() + "\"] delete from " + child.tableName + " where " + where.first + " (" + Arrays.toString(where.second) + ")");
                            int deleted = dbHelper.delete(child.tableName, where.first, where.second);
                            isSameChild &= deleted == 0;

                            if (deleted > 0)
                                break;
                        }
                    }
                }

                // Extract the current entity data from the object to be persisted
                ContentValues cv = currentEntity.toContentValues(obj, this);

                if (isSameChild) {
                    Pair<String, String[]> where = getWhereFilter(currentEntity.columns.getPrimaryKeys(), obj);
                    LogUtils.d("[Database \"" + database.getName() + "\"] update into " + currentEntity.tableName + " where " + where.first + " (" + Arrays.toString(where.second) + "): " + cv.toString());

                    dbHelper.update(currentEntity.tableName,
                            cv,
                            where.first,
                            where.second
                    );

                } else {
                    LogUtils.d("[Database \"" + database.getName() + "\"] insert into " + currentEntity.tableName + ": " + cv.toString());
                    dbHelper.insert(currentEntity.tableName, null, cv);
                }

                // Go up in the entity hierarchy
                currentEntity = currentEntity.getParent();
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
        if (postUpdate != null) {
            postUpdate.run(obj);
        }

        // Notify the observers
        for (WeakReference<LiveQuery<?>> observer : touchedObservers) {
            LiveQuery<?> query = observer.get();

            if (query != null) {
                query.refresh();
            }
        }
    }

    @Override
    public void remove(Object obj) {
        remove(obj, null, null);
    }

    @Override
    public <T> void remove(T obj, PreActionListener<T> preRemove, PostActionListener<T> postRemove) {
        database.waitUntilReady();

        // Pre remove actions
        if (preRemove != null) {
            preRemove.run(obj);
        }

        // Keep track of observers that should be notified
        Collection<WeakReference<LiveQuery<?>>> touchedObservers = new ArraySet<>();

        // Current working entity and the previous child entity
        EntityObject<?> currentEntity = database.getEntity(obj.getClass());

        // Open the database and start a transaction
        dbHelper.open();
        dbHelper.beginTransaction();

        try {
            while (currentEntity != null) {
                // Save the observers for this entity
                Collection<WeakReference<LiveQuery<?>>> obs = observers.get(currentEntity);

                if (obs != null) {
                    touchedObservers.addAll(obs);
                }

                // Remove
                Pair<String, String[]> where = getWhereFilter(currentEntity.columns.getPrimaryKeys(), obj);
                LogUtils.d("[Database \"" + database.getName() + "\"] delete from " + currentEntity.tableName + " where " + where.first + " (" + Arrays.toString(where.second) + ")");
                dbHelper.delete(currentEntity.tableName, where.first, where.second);

                // Go up in the entity hierarchy
                currentEntity = currentEntity.getParent();
            }

            dbHelper.setTransactionSuccessful();

        } catch (Exception e) {
            throw new QueryException(e);

        } finally {
            // End the transaction and close the database
            dbHelper.endTransaction();
            dbHelper.close();
        }

        // Post remove actions
        if (postRemove != null) {
            postRemove.run(obj);
        }

        // Notify the observers
        for (WeakReference<LiveQuery<?>> observer : touchedObservers) {
            LiveQuery<?> query = observer.get();

            if (query != null) {
                query.refresh();
            }
        }
    }

    /**
     * Get the application context.
     *
     * @return context
     * @throws IllegalStateException if the context is <code>null</code> (normally not happening
     *                               when dealing with the application context)
     */
    private Context getContext() {
        Context context = this.context.get();

        if (context == null) {
            throw new IllegalStateException("Application context is null");
        }

        return context;
    }

    /**
     * Register a live query as an observer for the entities it covers.<br>
     * If any of its observed entities is affected by a change, the query is re-executed in order
     * to retrieve the latest data from the database.
     *
     * @param query live query
     */
    public void registerLiveQuery(LiveQuery<?> query) {
        // Weak references are used in order to avoid query execution when their result
        // would not be used by anyone (for example if the activity died).

        WeakReference<LiveQuery<?>> weakReference = new WeakReference<>(query);

        for (EntityObject<?> entity : query.getObservedEntities()) {
            Collection<WeakReference<LiveQuery<?>>> observers = this.observers.get(entity);

            if (observers == null) {
                observers = new ArraySet<>();
                this.observers.put(entity, observers);
            }

            // Add the new observer
            observers.add(weakReference);

            // Remove expired observers
            Iterator<WeakReference<LiveQuery<?>>> iterator = observers.iterator();

            while (iterator.hasNext()) {
                if (iterator.next().get() == null)
                    iterator.remove();
            }
        }
    }

    /**
     * Get the selection clause corresponding to a given object and columns.
     *
     * @param columns columns to be used in the statement
     * @param obj     object from which the values have to be extracted
     * @return pair composed by statement and arguments
     */
    private static Pair<String, String[]> getWhereFilter(Collection<? extends BaseColumnObject> columns, Object obj) {
        StringBuilder where = new StringBuilder();
        List<String> whereArgs = new ArrayList<>(columns.size());

        for (BaseColumnObject column : columns) {
            if (where.length() > 0)
                where.append(" AND ");

            where.append(column.name).append(" = ?");
            whereArgs.add(String.valueOf(column.getValue(obj)));
        }

        return new Pair<>(where.toString(), whereArgs.toArray(new String[columns.size()]));
    }

}
