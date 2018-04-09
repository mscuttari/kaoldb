package it.mscuttari.kaoldb;

import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.DatabaseSchemaMigrator;

public class DatabaseObject {

    public String name;
    public Integer version;
    public Class<? extends DatabaseSchemaMigrator> migrator;
    public List<Class<?>> classes;
    public Map<Class<?>, EntityObject> entities;

}
