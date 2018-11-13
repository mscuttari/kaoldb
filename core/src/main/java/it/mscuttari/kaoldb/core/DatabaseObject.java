package it.mscuttari.kaoldb.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

class DatabaseObject {

    /** Database name */
    private String name;

    /** Database version */
    private Integer version;

    /** Schema migrator to be used for database version changed */
    private Class<? extends DatabaseSchemaMigrator> migrator;

    /** Entities */
    private final Collection<Class<?>> classes;

    /** Entities mapping */
    private final Map<Class<?>, EntityObject> entities;


    /**
     * Default constructor
     */
    public DatabaseObject() {
        this.classes = new HashSet<>();
        this.entities = new HashMap<>();
    }


    /**
     * Get database name
     *
     * @return  database name
     * @throws  InvalidConfigException if the name has not been set
     */
    public String getName() {
        if (name == null) {
            throw new InvalidConfigException("Database name not set");
        }

        return name;
    }


    /**
     * Set database name
     *
     * @param   name    database name
     * @throws  InvalidConfigException if the name is null or empty
     */
    public void setName(String name) {
        LogUtils.d("[Database]: setting name \"" + name + "\"");

        if (name == null) {
            throw new InvalidConfigException("Database name can't be null");
        } else if (name.trim().isEmpty()) {
            throw new InvalidConfigException("Database name can't be empty");
        }

        this.name = name.trim();
    }


    /**
     * Get database version
     *
     * @return  database version
     * @throws  InvalidConfigException if the version has not been set
     */
    public int getVersion() {
        if (version == null) {
            throw new InvalidConfigException("Database version not set");
        }

        return version;
    }


    /**
     * Set database version
     *
     * @param   version     database version
     * @throws  InvalidConfigException if the version is null or < 0
     */
    public void setVersion(Integer version) {
        LogUtils.d("[Database \"" + name + "\"]: setting version " + version);

        if (version == null) {
            throw new InvalidConfigException("Database version can't be null");
        } else if (version < 0) {
            throw new InvalidConfigException("Database version (" + version + ") is invalid");
        }

        this.version = version;
    }


    /**
     * Get database schema migrator
     *
     * @return  database schema migrator
     * @throws  InvalidConfigException if the database schema migrator has not been set
     */
    public Class<? extends DatabaseSchemaMigrator> getSchemaMigrator() {
        if (migrator == null) {
            throw new InvalidConfigException("Database schema migrator not set");
        }

        return migrator;
    }


    /**
     * Set database schema migrator
     *
     * @param   migrator        database schema migrator
     * @throws  InvalidConfigException if the schema migrator is null
     */
    public void setSchemaMigrator(Class<? extends DatabaseSchemaMigrator> migrator) {
        if (migrator == null) {
            throw new InvalidConfigException("Database schema migrator can't be null");
        }

        LogUtils.d("[Database \"" + name + "\"]: setting schema migrator " + migrator.getSimpleName());
        this.migrator = migrator;
    }


    /**
     * Get an unmodifiable {@link Collection} of all entities classes
     * To add new classes, use {@link #addEntityClass(Class)} or {@link #setEntityClasses(Collection)}
     *
     * @return  entity classes
     */
    public Collection<Class<?>> getEntityClasses() {
        return Collections.unmodifiableCollection(classes);
    }


    /**
     * Add entity class
     *
     * @param   clazz       entity class
     * @throws  InvalidConfigException if the class isn't annotated with {@link Entity}
     */
    public void addEntityClass(Class<?> clazz) {
        if (clazz == null)
            return;

        LogUtils.d("[Database \"" + name + "\"]: adding entity class " + clazz.getSimpleName());

        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new InvalidConfigException("Class " + clazz.getSimpleName() + " doesn't have @Entity annotation");
        }

        classes.add(clazz);
    }


    /**
     * Set entity classes
     *
     * @param   classes     entity classes
     * @throws  InvalidConfigException if any of the classes isn't annotated with {@link Entity}
     */
    public void setEntityClasses(Collection<Class<?>> classes) {
        this.classes.clear();

        for (Class<?> clazz : classes)
            addEntityClass(clazz);
    }


    /**
     * Get {@link EntityObject} corresponding to an entity class
     *
     * @param   entityClass             entity class
     * @return  {@link EntityObject} of the entity
     * @throws  InvalidConfigException  if the entity has not been found in the mapped ones
     */
    public EntityObject getEntity(Class<?> entityClass) {
        EntityObject entityObject = entities.get(entityClass);

        if (entityObject == null)
            throw new InvalidConfigException("Entity " + entityClass.getSimpleName() + " not found");

        return entityObject;
    }


    /**
     * Get an unmodifiable {@link Map} between the classes and their entity objects
     *
     * @return mapped entities
     */
    public Map<Class<?>, EntityObject> getEntitiesMap() {
        return Collections.unmodifiableMap(entities);
    }


    /**
     * Get an unmodifiable {@link Collection} of all entity objects
     * To set a map, use {@link #setEntitiesMap(Map)}
     *
     * @return  mapped entities
     */
    public Collection<EntityObject> getEntities() {
        return Collections.unmodifiableCollection(entities.values());
    }


    /**
     * Set entities mapping
     *
     * @param   map     map between entity class and {@link EntityObject}
     */
    public void setEntitiesMap(Map<Class<?>, EntityObject> map) {
        this.entities.clear();
        this.entities.putAll(map);
    }

}
