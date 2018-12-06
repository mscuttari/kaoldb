package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

/**
 * @param   <X>     entity root
 */
class From<X> implements Root<X> {

    protected final DatabaseObject db;
    protected final EntityObject entity;
    private final String alias;

    // Used during the SQL string build to keep track of the visited roots
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
        this.entity = db.getEntity(entityClass);
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
            EntityObject entity = db.getEntity(getEntityClass());

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
        return entity.clazz;
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
    public Expression isNull(SingleProperty<X, ?> field) {
        Variable<X, ?> a = new Variable<>(db, entity, alias, field);

        return PredicateImpl.isNull(db, a);
    }


    @Override
    public <T> Expression eq(SingleProperty<X, T> field, T value) {
        Variable<X, T> a = new Variable<>(db, entity, alias, field);
        Variable<?, T> b = new Variable<>(value);

        return PredicateImpl.eq(db, a, b);
    }


    @Override
    public <T> Expression eq(SingleProperty<X, T> x, SingleProperty<X, T> y) {
        Variable<X, T> a = new Variable<>(db, entity, alias, x);
        Variable<X, T> b = new Variable<>(db, entity, alias, y);

        return PredicateImpl.eq(db, a, b);
    }


    @Override
    public <Y, T> Expression eq(SingleProperty<X, T> x, Class<Y> yClass, String yAlias, SingleProperty<Y, T> y) {
        Variable<X, T> a = new Variable<>(db, entity, alias, x);
        Variable<Y, T> b = new Variable<>(db, db.getEntity(yClass), yAlias, y);

        return PredicateImpl.eq(db, a, b);
    }


    /**
     * Get full alias of this entity table (alias + class name)
     *
     * @return  full alias
     */
    public String getFullAlias() {
        return getFullAlias(getAlias(), getEntityClass());
    }


    /**
     * Get full alias of an entity table (alias + class name)
     *
     * @param   alias   entity alias
     * @param   clazz   entity class
     *
     * @return  full alias
     */
    public static String getFullAlias(String alias, Class<?> clazz) {
        return alias + clazz.getSimpleName();
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

                    for (BaseColumnObject primaryKey : parent.columns.getPrimaryKeys()) {
                        Variable<?, ?> a = new Variable<>(db, entity, alias, new SingleProperty<>(entity.clazz, primaryKey.type, primaryKey.field));
                        Variable<?, ?> b = new Variable<>(db, parent, alias, new SingleProperty<>(parent.clazz, primaryKey.type, primaryKey.field));
                        Expression onParent = PredicateImpl.eq(db, a, b);
                        on = on == null ? onParent : on.and(onParent);
                    }

                    if (on == null)
                        throw new QueryException("Can't merge inherited tables");

                    root = new InnerJoin<>(db, root, parent.clazz, alias, on);
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

                    for (BaseColumnObject primaryKey : entity.columns.getPrimaryKeys()) {
                        Variable<?, ?> a = new Variable<>(db, entity, alias, new SingleProperty<>(entity.clazz, primaryKey.type, primaryKey.field));
                        Variable<?, ?> b = new Variable<>(db, child, alias, new SingleProperty<>(child.clazz, primaryKey.type, primaryKey.field));
                        Expression onChild = PredicateImpl.eq(db, a, b);
                        on = on == null ? onChild : on.and(onChild);
                    }

                    if (on == null)
                        throw new QueryException("Can't merge inherited tables");

                    root = new LeftJoin<>(db, root, child.clazz, alias, on);
                    root.hierarchyVisited = true;
                    root = resolveChildrenInheritance(root, db.getEntity(root.getEntityClass()), alias);
                }
            }
        }

        return root;
    }

}
