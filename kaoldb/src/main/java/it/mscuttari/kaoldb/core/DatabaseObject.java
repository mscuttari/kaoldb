package it.mscuttari.kaoldb.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

class DatabaseObject {

    public String name;
    public Integer version;
    public Class<? extends DatabaseSchemaMigrator> migrator;
    public List<Class<?>> classes;
    public Map<Class<?>, EntityObject> entities;

}
