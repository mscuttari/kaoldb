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

package it.mscuttari.kaoldb.mapping;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.LogUtils;
import it.mscuttari.kaoldb.dump.DatabaseDumpImpl;
import it.mscuttari.kaoldb.exceptions.DatabaseManagementException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.interfaces.DatabaseDump;
import it.mscuttari.kaoldb.interfaces.SchemaAction;
import it.mscuttari.kaoldb.interfaces.TableDump;

import static it.mscuttari.kaoldb.ConcurrentSession.doAndNotifyAll;
import static it.mscuttari.kaoldb.ConcurrentSession.waitWhile;

/**
 * Each {@link DatabaseObject} maps a database.
 */
public class DatabaseObject {

    /** Database name */
    private String name;

    /** Database version */
    private Integer version;
    
    /** Schema migrator to be used for database version changes */
    private Class<? extends DatabaseSchemaMigrator> migrator;

    /** Entities */
    private final Collection<Class<?>> classes = new ArraySet<>();

    /** Entities mapping */
    private final Map<Class<?>, EntityObject<?>> entities = new ArrayMap<>();

    /** Whether the database version is being changed */
    private boolean updating = false;

    /** Whether the mapping process has been started at least once (it may be still running) */
    private boolean mappedOnce = false;

    /** Used to track the entities mapping. When 0, it means that all the entities have been mapped */
    private int mappingStatus = 0;

    /**
     * Get database name.
     *
     * @return database name
     * @throws IllegalStateException if the name has not been set
     */
    public String getName() {
        if (name == null) {
            throw new IllegalStateException("Database name not set");
        }

        return name;
    }

    /**
     * Set database name.
     *
     * @param name      database name
     * @throws IllegalArgumentException if the name is null or empty
     */
    public void setName(String name) {
        LogUtils.d("[Database] setting name \"" + name + "\"");

        if (name == null) {
            throw new IllegalArgumentException("Database name can't be null");
        } else if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name can't be empty");
        }

        this.name = name.trim();
    }

    /**
     * Get database version.
     *
     * @return database version
     * @throws IllegalStateException if the version has not been set
     */
    public int getVersion() {
        if (version == null) {
            throw new IllegalStateException("Database version not set");
        }

        return version;
    }

    /**
     * Set database version.
     *
     * @param version       database version
     * @throws IllegalArgumentException if the version is null or < 0
     */
    public void setVersion(Integer version) {
        LogUtils.d("[Database \"" + name + "\"] setting version " + version);

        if (version == null) {
            throw new IllegalArgumentException("Database version can't be null");
        } else if (version < 0) {
            throw new IllegalArgumentException("Database version (" + version + ") is invalid");
        }

        this.version = version;
    }

    /**
     * Get database schema migrator.
     *
     * @return database schema migrator
     * @throws IllegalStateException if the database schema migrator has not been set
     */
    public Class<? extends DatabaseSchemaMigrator> getSchemaMigrator() {
        if (migrator == null) {
            throw new IllegalStateException("Database schema migrator not set");
        }

        return migrator;
    }

    /**
     * Set database schema migrator.
     *
     * @param migrator      database schema migrator
     * @throws IllegalArgumentException if the schema migrator is null
     */
    public void setSchemaMigrator(Class<? extends DatabaseSchemaMigrator> migrator) {
        if (migrator == null) {
            throw new IllegalArgumentException("Database schema migrator can't be null");
        } else if (Modifier.isAbstract(migrator.getModifiers())) {
            throw new IllegalArgumentException("Database schema migrator can't be abstract");
        }

        LogUtils.d("[Database \"" + name + "\"] setting schema migrator " + migrator.getSimpleName());
        this.migrator = migrator;
    }

    /**
     * Add entity class.
     *
     * @param clazz     entity class
     * @throws IllegalArgumentException if the class isn't annotated with {@link Entity}
     */
    public void addEntityClass(Class<?> clazz) {
        if (clazz == null)
            return;

        LogUtils.d("[Database \"" + name + "\"] adding class \"" + clazz.getSimpleName() + "\"");

        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class \"" + clazz.getSimpleName() + "\" doesn't have @Entity annotation");
        }

        doAndNotifyAll(this, () -> classes.add(clazz));
    }

    /**
     * Check whether a class belongs to the database entities.
     *
     * @param clazz     class
     * @return <code>true</code> if the class is an entity; <code>false</code> otherwise
     */
    public synchronized boolean contains(Class<?> clazz) {
        return classes.contains(clazz);
    }

    /**
     * Get {@link EntityObject} corresponding to an entity class.
     *
     * @param clazz     entity class
     * @return entity object
     * @throws IllegalArgumentException if the entities have not been mapped yet and the entity class
     *                                  doesn't belong to this database
     */
    @SuppressWarnings("unchecked")
    public <T> EntityObject<T> getEntity(Class<T> clazz) {
        // Check if the entity should exists and eventually wait for it to be discovered, but only
        // if the mapping process has not completed yet.
        // When the mapping process is finished, all the constraint should be consistent by design
        // and therefore this check is not needed.

        if (!isMapped()) {
            // Check if the class is an entity of this database
            if (!contains(clazz)) {
                throw new IllegalArgumentException("Entity \"" + clazz.getSimpleName() + "\" not found");
            }

            // Wait for the entity to be mapped
            waitWhile(this, () -> entities.get(clazz) == null);
        }

        return (EntityObject<T>) entities.get(clazz);
    }

    /**
     * Get an unmodifiable {@link Collection} of all the entity objects.
     *
     * @return mapped entities
     */
    public Collection<EntityObject<?>> getEntities() {
        if (!isMapped()) {
            waitWhile(this, () -> entities.size() != classes.size());
        }

        return Collections.unmodifiableCollection(entities.values());
    }

    /**
     * Check whether the entities have been mapped.
     *
     * @return <code>true</code> if the entities have been mapped at least once;
     *         <code>false</code> otherwise
     */
    private boolean isMapped() {
        return mappedOnce && !isMapping();
    }

    /**
     * Check whether the mapping process is currently executing.
     *
     * @return <code>true</code> if entities are being mapped; <code>false</code> otherwise
     */
    private boolean isMapping() {
        return mappingStatus != 0;
    }

    /**
     * Register the fact that an entity has been completely mapped.
     *
     * @see #mappingStatus
     */
    public void entityMapped() {
        doAndNotifyAll(this, () -> {
            if (--mappingStatus == 0) {
                LogUtils.d("[Database \"" + name + "\"] " + entities.size() + " entities mapped");
            }
        });
    }

    /**
     * Check whether the database is ready for use.
     *
     * <p>The database is considered ready if the mapping process has finished and the
     * database version is not being upgraded or downgraded</p>
     *
     * @return <code>true</code> if the database is ready; <code>false</code> otherwise
     */
    public boolean isReady() {
        return isMapped() && !updating;
    }

    /**
     * Block the calling thread until the database becomes ready.
     */
    public void waitUntilReady() {
        waitWhile(this, () -> !isReady());
    }

    /**
     * Create an {@link EntityObject} for each class contained in {@link #classes}.
     */
    public void mapEntities() {
        if (isMapped()) {
            LogUtils.w("[Database \"" + name + "\"] entities already mapped");

        } else if (isMapping()) {
            LogUtils.w("[Database \"" + name + "\"] entities are already being mapped");

        } else {
            mappingStatus = classes.size();

            doAndNotifyAll(this, () -> {
                for (Class<?> clazz : classes) {
                    entities.put(clazz, EntityObject.map(this, clazz));
                }

                mappedOnce = true;
            });
        }
    }

    /**
     * Create the database.
     *
     * @param db    writable database
     * @throws IllegalStateException if the entities have not been mapped yet and there is no
     *                               mapping process going on
     */
    public void create(SQLiteDatabase db) {
        if (!isMapped()) {
            if (isMapping()) {
                // Wait for the mapping process to end
                waitWhile(this, () -> !this.isMapped());

            } else {
                // There is no mapping process going on
                throw new IllegalStateException("Database not mapped");
            }
        }

        try {
            db.beginTransaction();

            for (EntityObject<?> entity : getEntities()) {
                // Entity table
                String entityTableCreateSQL = entity.getSQL();

                if (entityTableCreateSQL != null) {
                    LogUtils.d("[Entity \"" + entity.getName() + "\"] " + entityTableCreateSQL);
                    db.execSQL(entityTableCreateSQL);
                    LogUtils.i("[Entity \"" + entity.getName() + "\"] table created");
                }

                // Join tables
                for (Relationship relationship : entity.relationships) {
                    if (!relationship.field.isAnnotationPresent(JoinTable.class))
                        continue;

                    JoinTableObject joinTableObject = new JoinTableObject(this, entity, relationship.field);
                    joinTableObject.map();
                    joinTableObject.waitUntilMapped();

                    String joinTableCreateSQL = joinTableObject.getSQL();
                    LogUtils.d("[Entity \"" + entity.getName() + "\"] " + joinTableCreateSQL);
                    db.execSQL(joinTableCreateSQL);
                    LogUtils.i("[Entity \"" + entity.getName() + "\"] join table created");
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            throw new DatabaseManagementException(e);

        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            }
        }
    }

    /**
     * Upgrade the database.
     *
     * @param db            writable database
     * @param oldVersion    old version
     * @param newVersion    new version
     */
    public void upgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.d("[Database \"" + getName() + "\"] upgrading from version " + oldVersion + " to version " + newVersion);

        doAndNotifyAll(this, () -> updating = true);
        db.beginTransaction();

        try {
            DatabaseSchemaMigrator migrator = getSchemaMigrator().newInstance();

            // Apply changes one version by one
            for (int i = oldVersion; i < newVersion; i++) {
                List<SchemaAction> actions = migrator.onUpgrade(i, i + 1, getDump(db));

                if (actions != null) {
                    for (SchemaAction action : actions) {
                        action.run(db);
                    }
                }
            }

            // Commit the changes
            db.setTransactionSuccessful();

        } catch (Exception e) {
            throw new DatabaseManagementException(e);

        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            }

            doAndNotifyAll(this, () -> updating = false);
        }

        LogUtils.i("[Database \"" + getName() + "\"] upgraded from version " + oldVersion + " to version " + newVersion);
    }

    /**
     * Downgrade the database.
     *
     * @param db            writable database
     * @param oldVersion    old version
     * @param newVersion    new version
     */
    public void downgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtils.d("[Database \"" + getName() + "\"] downgrading from version " + oldVersion + " to version " + newVersion);

        doAndNotifyAll(this, () -> updating = true);
        db.beginTransaction();

        try {
            DatabaseSchemaMigrator migrator = getSchemaMigrator().newInstance();

            for (int i = oldVersion; i > newVersion; i--) {
                List<SchemaAction> actions = migrator.onDowngrade(i, i - 1, getDump(db));

                if (actions != null) {
                    for (SchemaAction action : actions) {
                        action.run(db);
                    }
                }
            }

            // Commit the changes
            db.setTransactionSuccessful();

        } catch (Exception e) {
            throw new DatabaseManagementException(e);

        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            }

            doAndNotifyAll(this, () -> updating = false);
        }

        LogUtils.i("[Database \"" + getName() + "\"] downgraded from version " + oldVersion + " to version " + newVersion);
    }

    /**
     * Get database dump.
     *
     * @param db    readable database
     * @return database dump
     */
    public DatabaseDump getDump(SQLiteDatabase db) {
        return new DatabaseDumpImpl(db);
    }

    /**
     * Restore database dump.
     *
     * @param db    writable database
     * @param dump  database dump
     *
     * @throws DatabaseManagementException if the database is read-only
     */
    public void restore(SQLiteDatabase db, DatabaseDump dump) {
        if (db.isReadOnly()) {
            throw new DatabaseManagementException("Database not writable");
        }

        deleteAllTables(db);

        for (TableDump table : dump.getTables()) {
            //db.rawQuery(table.get)
        }
    }

    /**
     * Delete all the database tables.
     *
     * @param db    writable database
     * @throws DatabaseManagementException if the database is read-only
     */
    private void deleteAllTables(SQLiteDatabase db) {
        if (db.isReadOnly()) {
            throw new DatabaseManagementException("Database not writable");
        }

        try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)) {
            List<String> tables = new ArrayList<>(c.getCount());

            while (c.moveToNext()) {
                tables.add(c.getString(0));
            }

            for (String table : tables) {
                if (table.startsWith("sqlite_")) {
                    continue;
                }

                db.execSQL("DROP TABLE IF EXISTS " + table);
                LogUtils.i("[Table \"" + table + "\"] dropped");
            }
        }
    }

}
