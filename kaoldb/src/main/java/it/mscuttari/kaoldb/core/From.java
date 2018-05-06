package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

class From<X> implements Root<X> {

    protected DatabaseObject db;
    protected EntityObject entity;
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
    public String getTableAlias() {
        return alias;
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Property<X, Y> property) {
        return new InnerJoin<>(db, this, entityClass, alias, property);
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Expression on) {
        return new InnerJoin<>(db, this, entityClass, alias, on);
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Property<X, Y> property) {
        return new LeftJoin<>(db, this, entityClass, alias, property);
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Expression on) {
        return new LeftJoin<>(db, this, entityClass, alias, on);
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> naturalJoin(Class<Y> entityClass, String alias, Property<X, Y> property) {
        return new NaturalJoin<>(db, this, entityClass, alias, property);
    }


    /** {@inheritDoc} */
    @Override
    public <Y> Root<Y> naturalJoin(Class<Y> entityClass, String alias) {
        return new NaturalJoin<>(db, this, entityClass, alias);
    }


    /** {@inheritDoc} */
    @Override
    public Expression isNull(Property<X, ?> field) {
        Variable<X, ?> a = new Variable<>(db, entity, alias, field);

        return PredicateImpl.isNull(db, a);
    }


    /** {@inheritDoc} */
    @Override
    public <T> Expression eq(Property<X, T> field, T value) {
        Variable<X, T> a = new Variable<>(db, entity, alias, field);
        Variable<?, T> b = new Variable<>(value);

        return PredicateImpl.eq(db, a, b);
    }


    /** {@inheritDoc} */
    @Override
    public <T> Expression eq(Property<X, T> x, Property<X, T> y) {
        Variable<X, T> a = new Variable<>(db, entity, alias, x);
        Variable<X, T> b = new Variable<>(db, entity, alias, y);

        return PredicateImpl.eq(db, a, b);
    }


    /** {@inheritDoc} */
    @Override
    public <Y, T> Expression eq(Property<X, T> x, Class<Y> yClass, String yAlias, Property<Y, T> y) {
        Variable<X, T> a = new Variable<>(db, entity, alias, x);

        if (!db.entities.containsKey(yClass))
            throw new QueryException("Class " + yClass.getSimpleName() + " is not an entity");

        Variable<Y, T> b = new Variable<>(db, db.entities.get(yClass), yAlias, y);

        return PredicateImpl.eq(db, a, b);
    }


    /**
     * Get entity object
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
