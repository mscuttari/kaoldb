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

package it.mscuttari.kaoldb.core;

import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.exceptions.MappingException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;
import it.mscuttari.kaoldb.interfaces.DatabaseDump;

import static it.mscuttari.kaoldb.core.ConcurrentSession.doAndNotifyAll;
import static it.mscuttari.kaoldb.core.ConcurrentSession.waitWhile;

/**
 * Each {@link DatabaseObject} maps a database
 */
class DatabaseObject {

    /** Database name */
    private String name;

    /** Database version */
    private Integer version;

    /** Schema migrator to be used for database version changes */
    private Class<? extends DatabaseSchemaMigrator> migrator;

    /** Entities */
    private final Collection<Class<?>> classes = new HashSet<>();

    /** Entities mapping */
    private final Map<Class<?>, EntityObject<?>> entities = new HashMap<>();

    /** Whether the database version is being changed */
    public final AtomicBoolean upgrading = new AtomicBoolean(false);

    /** Whether the entities have been mapped or not */
    private boolean mapped = false;


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
     * Get an unmodifiable {@link Collection} of all entities classes.
     * To add new classes, use {@link #addEntityClass(Class)}.
     *
     * @return entity classes
     */
    public Collection<Class<?>> getEntityClasses() {
        return Collections.unmodifiableCollection(classes);
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
     * Get {@link EntityObject} corresponding to an entity class.
     *
     * @param clazz     entity class
     * @return entity object
     * @throws IllegalArgumentException if the entities have not been mapped yet and the entity class
     *                                  doesn't belong to this database
     */
    @SuppressWarnings("unchecked")
    public <T> EntityObject<T> getEntity(Class<T> clazz) {
        // Check if the entity should exists and eventually wait for it to be mapped, but only
        // if the entities have not been mapped yet.
        // When the mapping process is finished, all the constraint should be consistent by design
        // and therefore this check is not needed.

        if (!mapped) {
            // Check if the class is an entity of this database
            if (!classes.contains(clazz)) {
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
        return Collections.unmodifiableCollection(entities.values());
    }


    /**
     * Create an {@link EntityObject} for each class contained in {@link #classes}.
     *
     * <p>
     * The mapping process is done in two phases:
     * <ol>
     *      <li>Get the basic entity data in order to establish the paternity relationships</li>
     *      <li>Determine the columns of each table (own columns and inherited ones)</li>
     * </ol>
     * </p>
     *
     * @throws MappingException in case of mapping error
     */
    public void mapEntities() {
        LogUtils.d("[Database \"" + name + "\"] mapping the entities");

        mapped = false;
        doAndNotifyAll(this, entities::clear);

        try {
            // First scan to get basic data
            ConcurrentSession entitiesConcurrentSession = new ConcurrentSession();
            LogUtils.v("[Database \"" + name + "\"] entities mapping: first scan to get basic data");

            for (Class<?> clazz : classes) {
                Runnable action = () -> entities.put(clazz, EntityObject.map(this, clazz));
                entitiesConcurrentSession.submit(() -> doAndNotifyAll(this, action));
            }

            entitiesConcurrentSession.waitForAll();

            // Second scan to load the columns
            ConcurrentSession columnsConcurrentSession = new ConcurrentSession();
            LogUtils.v("[Database \"" + name + "\"] entities mapping: second scan to load the columns");

            for (EntityObject<?> entity : entities.values()) {
                columnsConcurrentSession.submit(entity::loadColumns);
            }

            columnsConcurrentSession.waitForAll();

            mapped = true;
            LogUtils.d("[Database \"" + name + "\"] " + entities.size() + " entities mapped");

        } catch (Exception e) {
            throw new MappingException(e);
        }
    }


    /**
     * Get database dump.
     *
     * @return database dump
     */
    public DatabaseDump getDump(SQLiteDatabase db) {
        return new DatabaseDumpImpl(db);
    }

}
