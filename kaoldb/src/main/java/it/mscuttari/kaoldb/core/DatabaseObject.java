package it.mscuttari.kaoldb.core;

import java.util.Collection;
import java.util.Map;

import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

class DatabaseObject {

    /** Database name */
    public String name;

    /** Database version */
    public Integer version;

    /** Schema migrator to be used for database version changed */
    public Class<? extends DatabaseSchemaMigrator> migrator;

    /** Entities */
    public Collection<Class<?>> classes;

    /** Entities mapping */
    public Map<Class<?>, EntityObject> entities;

}
