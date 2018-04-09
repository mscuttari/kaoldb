package it.mscuttari.kaoldb.query;

import it.mscuttari.kaoldb.DatabaseObject;
import it.mscuttari.kaoldb.EntityObject;
import it.mscuttari.kaoldb.exceptions.QueryException;

public class From {

    private DatabaseObject db;
    protected EntityObject entity;
    private String alias;

    public From(DatabaseObject db, Class<?> entityClass) {
        this(db, entityClass, entityClass.getSimpleName());
    }

    public From(DatabaseObject db, Class<?> entityClass, String alias) {
        this.db = db;
        this.entity = getEntity(entityClass);
        this.alias = alias;
    }

    @Override
    public String toString() {
        return entity.tableName + " AS " + alias;
    }


    public From join(Class<?> entityClass) {
        return join(entityClass, entityClass.getSimpleName());
    }


    public From join(Class<?> entityClass, String alias) {
        return new Join(db, this, entityClass, alias);
    }


    /**
     * Get the entity of an object
     *
     * @param   entityClass     entity class
     * @return  entity object
     * @throws QueryException if the class is not an entity
     */
    private EntityObject getEntity(Class<?> entityClass) {
        EntityObject entity = db.entities.get(entityClass);

        if (entity == null)
            throw new QueryException("Class " + entityClass.getSimpleName() + " is not an entity");

        return entity;
    }

    protected String columnWithAlias(String columnName) {
        return alias + "." + columnName;
    }

}
