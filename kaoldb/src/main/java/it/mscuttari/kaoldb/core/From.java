package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

/**
 * @param   <X>     entity root
 */
class From<X> implements Root<X> {

    protected DatabaseObject db;
    protected EntityObject entity;
    private String alias;
    private boolean hierarchyVisited = false;


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
        // Resolve hierarchy joins
        if (!hierarchyVisited && (entity.parent != null || entity.children.size() > 0)) {
            From<?> root = this;
            EntityObject entity = getEntity(getEntityClass());

            root = resolveParentInheritance(root, entity, alias);
            root = resolveChildrenInheritance(root, entity, alias);

            return root.toString();
        }

        try {
            return entity.tableName + " AS " + alias + getEntityClass().getSimpleName();
        } finally {
            // Reset the status ot allow a second query build
            hierarchyVisited = false;
        }
    }


    @Override
    public Class<?> getEntityClass() {
        return entity.entityClass;
    }


    @Override
    public String getAlias() {
        return alias;
    }


    @Override
    public <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Property<X, Y> property) {
        return new InnerJoin<>(db, this, entityClass, alias, property);
    }


    @Override
    public <Y> Root<Y> innerJoin(Class<Y> entityClass, String alias, Expression on) {
        return new InnerJoin<>(db, this, entityClass, alias, on);
    }


    @Override
    public <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Property<X, Y> property) {
        return new LeftJoin<>(db, this, entityClass, alias, property);
    }


    @Override
    public <Y> Root<Y> leftJoin(Class<Y> entityClass, String alias, Expression on) {
        return new LeftJoin<>(db, this, entityClass, alias, on);
    }


    @Override
    public <Y> Root<Y> naturalJoin(Class<Y> entityClass, String alias) {
        return new NaturalJoin<>(db, this, entityClass, alias);
    }


    @Override
    public Expression isNull(Property<X, ?> field) {
        Variable<X, ?> a = new Variable<>(db, entity, alias, field);

        return PredicateImpl.isNull(db, a);
    }


    @Override
    public <T> Expression eq(Property<X, T> field, T value) {
        Variable<X, T> a = new Variable<>(db, entity, alias, field);
        Variable<?, T> b = new Variable<>(value);

        return PredicateImpl.eq(db, a, b);
    }


    @Override
    public <T> Expression eq(Property<X, T> x, Property<X, T> y) {
        Variable<X, T> a = new Variable<>(db, entity, alias, x);
        Variable<X, T> b = new Variable<>(db, entity, alias, y);

        return PredicateImpl.eq(db, a, b);
    }


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


    /**
     * Merge parent tables
     *
     * @param   root        main root
     * @param   entity      main entity object
     * @param   alias       main alias
     *
     * @return  join root
     */
    private From<?> resolveParentInheritance(From<?> root, EntityObject entity, String alias) {
        root.hierarchyVisited = true;

        if (entity.parent != null) {
            EntityObject parent = entity.parent;

            while (parent != null) {
                if (parent.inheritanceType != InheritanceType.SINGLE_TABLE) {
                    Expression on = null;

                    for (ColumnObject primaryKey : parent.primaryKeys) {
                        if (primaryKey.field == null)
                            throw new InvalidConfigException("Primary key field not found");

                        Variable<?, ?> a = new Variable<>(db, entity, alias, new Property<>(entity.entityClass, primaryKey.type, primaryKey.field.getName()));
                        Variable<?, ?> b = new Variable<>(db, parent, alias, new Property<>(parent.entityClass, primaryKey.type, primaryKey.field.getName()));
                        Expression onParent = PredicateImpl.eq(db, a, b);
                        on = on == null ? onParent : on.and(onParent);
                    }

                    if (on == null)
                        throw new QueryException("Can't merge inherited tables");

                    root = new InnerJoin<>(db, root, parent.entityClass, alias, on);
                    root.hierarchyVisited = true;
                }

                parent = parent.parent;
            }
        }

        return root;
    }


    /**
     * Merge children tables
     *
     * @param   root        main root
     * @param   entity      current entity object
     * @param   alias       main alias
     *
     * @return  join root
     */
    private From<?> resolveChildrenInheritance(From<?> root, EntityObject entity, String alias) {
        root.hierarchyVisited = true;

        if (entity.children.size() != 0) {
            for (EntityObject child : entity.children) {
                if (child.inheritanceType != InheritanceType.SINGLE_TABLE) {
                    Expression on = null;

                    for (ColumnObject primaryKey : entity.primaryKeys) {
                        if (primaryKey.field == null)
                            throw new InvalidConfigException("Primary key field not found");

                        Variable<?, ?> a = new Variable<>(db, entity, alias, new Property<>(entity.entityClass, primaryKey.type, primaryKey.field.getName()));
                        Variable<?, ?> b = new Variable<>(db, child, alias, new Property<>(child.entityClass, primaryKey.type, primaryKey.field.getName()));
                        Expression onChild = PredicateImpl.eq(db, a, b);
                        on = on == null ? onChild : on.and(onChild);
                    }

                    if (on == null)
                        throw new QueryException("Can't merge inherited tables");

                    root = new LeftJoin<>(db, root, child.entityClass, alias, on);
                    root.hierarchyVisited = true;
                    root = resolveChildrenInheritance(root, db.entities.get(root.getEntityClass()), alias);
                }
            }
        }

        return root;
    }

}
