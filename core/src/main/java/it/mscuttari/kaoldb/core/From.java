package it.mscuttari.kaoldb.core;

import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @param   <X>     entity root
 */
final class From<X> implements Root<X> {

    @NonNull private final DatabaseObject db;
    @NonNull private final EntityObject<X> entity;
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
        this.entity = db.getEntity(checkNotNull(entityClass));
        this.alias = checkNotNull(alias);
    }


    /**
     * Get string representation to be used in query
     *
     * @return "FROM" clause
     */
    @Override
    public String toString() {
        try {
            // Resolve hierarchy joins if the hierarchy tree has not been visited yet

            if (!hierarchyVisited && (entity.parent != null || entity.children.size() > 0)) {
                Root<X> root = this;
                EntityObject<X> entity = db.getEntity(getEntityClass());
                hierarchyVisited = true;

                // Merge parent tables
                if (entity.parent != null) {
                    EntityObject<? super X> parent = entity.parent;

                    while (parent != null) {
                        if (parent.inheritanceType != InheritanceType.SINGLE_TABLE) {
                            Expression on = null;

                            for (BaseColumnObject primaryKey : parent.columns.getPrimaryKeys()) {
                                Variable<?> a = new Variable<>(alias, new SingleProperty<>(entity.clazz, primaryKey.type, primaryKey.field));
                                Variable<?> b = new Variable<>(alias, new SingleProperty<>(parent.clazz, primaryKey.type, primaryKey.field));

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

                // Merge children tables
                if (entity.children.size() != 0) {
                    Stack<EntityObject<? extends X>> stack = new Stack<>();
                    stack.push(entity);

                    while (!stack.empty()) {
                        EntityObject<? extends X> node = stack.pop();

                        for (EntityObject<? extends X> child : node.children) {
                            if (child.inheritanceType != InheritanceType.SINGLE_TABLE) {
                                // Perform the join with the child table
                                Expression on = null;

                                for (BaseColumnObject primaryKey : entity.columns.getPrimaryKeys()) {
                                    Variable<?> a = new Variable<>(alias, new SingleProperty<>(entity.clazz, primaryKey.type, primaryKey.field));
                                    Variable<?> b = new Variable<>(alias, new SingleProperty<>(child.clazz, primaryKey.type, primaryKey.field));

                                    Expression onChild = PredicateImpl.eq(db, a, b);
                                    on = on == null ? onChild : on.and(onChild);
                                }

                                if (on == null)
                                    throw new QueryException("Can't merge inherited tables");

                                From<?> childRoot = new From<>(db, child.clazz, alias);
                                childRoot.hierarchyVisited = true;

                                root = new Join<>(db, Join.JoinType.LEFT, root, childRoot, on);

                                // Depth first scan
                                if (child.children.size() != 0) {
                                    stack.push(child);
                                }
                            }
                        }

                    }
                }

                return root.toString();
            }

            // Tree exploration not needed
            return entity.tableName + " AS " + getFullAlias();

        } finally {
            // Reset the status ot allow a second query build
            hierarchyVisited = false;
        }
    }


    @Override
    public Class<X> getEntityClass() {
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
    public <Y> Root<X> join(@NonNull Root<Y> root, @NonNull Property<X, Y> property) {
        return new Join<>(db, Join.JoinType.INNER, this, root, property);
    }


    @Override
    public Expression isNull(@NonNull SingleProperty<X, ?> field) {
        Variable<?> a = new Variable<>(alias, field);

        return PredicateImpl.isNull(db, a);
    }


    @Override
    public <T> Expression eq(@NonNull SingleProperty<X, T> field, @Nullable T value) {
        Variable<T> a = new Variable<>(alias, field);
        Variable<T> b = new Variable<>(value);

        // Just in case the user wants to check for a null property but wrongly calls this
        // method instead of isNull
        if (value == null) {
            return PredicateImpl.isNull(db, a);
        }

        return PredicateImpl.eq(db, a, b);
    }


    @Override
    public <T> Expression eq(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.eq(db, a, b);
    }


    @Override
    public <Y, T> Expression eq(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(yAlias, y);

        return PredicateImpl.eq(db, a, b);
    }


    @Override
    public <T> Expression gt(@NonNull SingleProperty<X, T> field, @NonNull T value) {
        Variable<T> a = new Variable<>(alias, field);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.gt(db, a, b);
    }


    @Override
    public <T> Expression gt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.gt(db, a, b);
    }


    @Override
    public <Y, T> Expression gt(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(yAlias, y);

        return PredicateImpl.gt(db, a, b);
    }


    @Override
    public <T> Expression ge(@NonNull SingleProperty<X, T> field, @NonNull T value) {
        Variable<T> a = new Variable<>(alias, field);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.ge(db, a, b);
    }


    @Override
    public <T> Expression ge(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.ge(db, a, b);
    }


    @Override
    public <Y, T> Expression ge(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(yAlias, y);

        return PredicateImpl.ge(db, a, b);
    }


    @Override
    public <T> Expression lt(@NonNull SingleProperty<X, T> field, @NonNull T value) {
        Variable<T> a = new Variable<>(alias, field);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.lt(db, a, b);
    }


    @Override
    public <T> Expression lt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.lt(db, a, b);
    }


    @Override
    public <Y, T> Expression lt(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(yAlias, y);

        return PredicateImpl.lt(db, a, b);
    }


    @Override
    public <T> Expression le(@NonNull SingleProperty<X, T> field, @NonNull T value) {
        Variable<T> a = new Variable<>(alias, field);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.le(db, a, b);
    }


    @Override
    public <T> Expression le(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.le(db, a, b);
    }


    @Override
    public <Y, T> Expression le(@NonNull SingleProperty<X, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(yAlias, y);

        return PredicateImpl.le(db, a, b);
    }

}
