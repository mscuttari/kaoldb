package it.mscuttari.kaoldb.core;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

/**
 * @param   <X>     entity root
 */
final class From<X> implements Root<X> {

    @NonNull private final DatabaseObject db;
    @NonNull private final EntityObject entity;
    @NonNull private final String alias;

    // Used during the SQL string build to keep track of the visited roots
    private boolean hierarchyVisited = false;


    /**
     * Constructor
     *
     * @param db            database object
     * @param entityClass   entity class
     * @param alias         table alias
     */
    From(@NonNull DatabaseObject db,
         @NonNull Class<X> entityClass,
         @NonNull String alias) {

        this.db = db;
        this.entity = db.getEntity(entityClass);
        this.alias = alias;
    }


    /**
     * Get string representation to be used in query
     *
     * @return "FROM" clause
     */
    @Override
    public String toString() {
        // Resolve hierarchy joins
        if (!hierarchyVisited && (entity.parent != null || entity.children.size() > 0)) {
            Root<?> root = this;
            EntityObject entity = db.getEntity(getEntityClass());

            root = resolveParentInheritance(root, entity, alias);
            root = resolveChildrenInheritance(root, entity, alias);

            return root.toString();
        }

        try {
            return entity.tableName + " AS " + getFullAlias();
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
    public String getFullAlias() {
        return getFullAlias(getAlias(), getEntityClass());
    }


    /**
     * Get full alias of an entity table (alias + class name)
     *
     * @param alias     entity alias
     * @param clazz     entity class
     *
     * @return full alias
     */
    public static String getFullAlias(String alias, Class<?> clazz) {
        return alias + clazz.getSimpleName();
    }


    @Override
    public <Y> Root<X> join(Root<Y> root, Property<X, Y> property) {
        return new Join<>(db, Join.JoinType.INNER, this, root, property);
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
     * Merge parent tables
     *
     * @param root      main root
     * @param entity    main entity object
     * @param alias     main alias
     *
     * @return join root
     */
    private Root<?> resolveParentInheritance(Root<?> root, EntityObject entity, String alias) {
        if (root instanceof From<?>)
            ((From<?>) root).hierarchyVisited = true;

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

                    From<?> parentRoot = new From<>(db, parent.clazz, alias);
                    parentRoot.hierarchyVisited = true;

                    root = new Join<>(db, Join.JoinType.INNER, root, parentRoot, on);
                }

                parent = parent.parent;
            }
        }

        return root;
    }


    /**
     * Merge children tables
     *
     * @param root      main root
     * @param entity    current entity object
     * @param alias     main alias
     *
     * @return join root
     */
    private Root<?> resolveChildrenInheritance(Root<?> root, EntityObject entity, String alias) {
        if (root instanceof From<?>)
            ((From<?>) root).hierarchyVisited = true;

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

                    From<?> childRoot = new From<>(db, child.clazz, alias);
                    childRoot.hierarchyVisited = true;

                    root = new Join<>(db, Join.JoinType.LEFT, root, childRoot, on);
                    root = resolveChildrenInheritance(root, child, alias);
                }
            }
        }

        return root;
    }

}
