package it.mscuttari.kaoldb;

import java.util.List;

class DatabaseObject {

    String name;
    Integer version;
    Class<? extends DatabaseSchemaMigrator> migrator;
    List<Class<?>> classes;
    List<EntityObject> entities;

}
