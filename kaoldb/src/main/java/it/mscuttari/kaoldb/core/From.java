package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

class From<X> implements Root<X> {

    private DatabaseObject db;
    private EntityObject entity;
    private String alias;


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   entityClass     entity class
     * @param   alias           table alias
     */
    From(DatabaseObject db, Class<X> entityClass, String alias) {
        this.db = db;
        this.entity = getEntity(entityClass);
        this.alias = alias;
    }


    /**
     * Get string representation to be used in query
     *
     * @return  "from" clause
     */
    @Override
    public String toString() {
        return entity.tableName + " AS " + alias;
    }


    /** {@inheritDoc} */
    @Override
    public Class<?> getEntityClass() {
        return entity.entityClass;
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Expression on) {
        return new InnerJoin<>(db, this, entityClass, alias, on);
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Expression on) {
        return new LeftJoin<>(db, this, entityClass, alias, on);
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> naturalJoin(Class<Y> entityClass, String alias) {
        return new NaturalJoin<>(db, this, entityClass, alias);
    }


    /** {@inheritDoc} */
    @Override
    public Expression eq(Property field, Object value) {
        Variable a = new Variable(db, entity, alias, field);
        Variable b = new Variable(value);

        return PredicateImpl.eq(a, b);
    }


    /** {@inheritDoc} */
    @Override
    public Expression eq(Property x, Property y) {
        Variable a = new Variable(db, entity, alias, x);
        Variable b = new Variable(db, entity, alias, y);

        return PredicateImpl.eq(a, b);
    }


    /**
     * Get column name prefixed by the alias
     *
     * @param   columnName      column name
     * @return  alias.column
     */
    protected String columnWithAlias(String columnName) {
        return alias + "." + columnName;
    }


    /**
     * Get the entity of an object
     *
     * @param   entityClass     entity class
     * @return  entity object
     * @throws  QueryException  if the class is not an entity
     */
    private EntityObject getEntity(Class<?> entityClass) {
        EntityObject entity = db.entities.get(entityClass);

        if (entity == null)
            throw new QueryException("Class " + entityClass.getSimpleName() + " is not an entity");

        return entity;
    }

}
