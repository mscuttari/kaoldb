/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.core;

import java.util.Collections;
import java.util.Map;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.mscuttari.kaoldb.core.StringUtils.escape;

/**
 * @param   <X>     entity root
 */
final class From<X> implements RootInt<X> {

    @NonNull private final DatabaseObject db;
    @NonNull private final QueryBuilder<?> queryBuilder;
    @NonNull private final EntityObject<X> entity;
    @NonNull private final String alias;

    // Used during the SQL string build to keep track of the visited roots.
    // The variable value is thread local because multiple queries may be concurrently building.

    private ThreadLocal<Boolean> hierarchyVisited = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };


    /**
     * Constructor.
     *
     * @param db            database
     * @param queryBuilder  query builder
     * @param entityClass   entity class
     * @param alias         table alias
     */
    From(@NonNull DatabaseObject db,
         @NonNull QueryBuilder<?> queryBuilder,
         @NonNull Class<X> entityClass,
         @NonNull String alias) {

        this.db = db;
        this.queryBuilder = queryBuilder;
        this.entity = db.getEntity(checkNotNull(entityClass));
        this.alias = checkNotNull(alias);
    }


    /**
     * Get string representation to be used in query.
     * The parent and children table are automatically joined.
     *
     * @return <code>FROM</code> clause
     */
    @Override
    public String toString() {
        try {
            // Resolve hierarchy joins if the hierarchy tree has not been visited yet

            if (Boolean.valueOf(false).equals(hierarchyVisited.get()) && (entity.parent != null || entity.children.size() > 0)) {
                hierarchyVisited.set(true);
                RootInt<X> root = this;
                EntityObject<X> entity = db.getEntity(getEntityClass());

                // Merge parent tables
                if (entity.parent != null) {
                    EntityObject<? super X> parent = entity.parent;

                    while (parent != null) {
                        From<?> parentRoot = new From<>(db, queryBuilder, parent.clazz, getAlias() + parent.getName());
                        Expression on = null;

                        for (BaseColumnObject primaryKey : parent.columns.getPrimaryKeys()) {
                            Variable a = new Variable<>(alias, new SingleProperty<>(entity.clazz, primaryKey.type, primaryKey.field));
                            Variable b = new Variable<>(parentRoot.getAlias(), new SingleProperty<>(parent.clazz, primaryKey.type, primaryKey.field));

                            Expression onParent = PredicateImpl.eq(db, this, a, b);
                            on = on == null ? onParent : on.and(onParent);
                        }

                        if (on == null)
                            throw new QueryException("Can't merge inherited tables");

                        parentRoot.hierarchyVisited.set(true);

                        root = new Join<>(db, Join.JoinType.INNER, root, parentRoot, on);

                        parent = parent.parent;
                    }
                }

                // Merge children tables
                if (entity.children.size() != 0) {
                    Stack<EntityObject<? extends X>> children = new Stack<>();
                    children.push(entity);

                    while (!children.empty()) {
                        EntityObject<? extends X> node = children.pop();

                        for (EntityObject<? extends X> child : node.children) {
                            // Perform the join with the child table
                            From<?> childRoot = new From<>(db, queryBuilder, child.clazz, getAlias() + child.getName());
                            Expression on = null;

                            for (BaseColumnObject primaryKey : entity.columns.getPrimaryKeys()) {
                                Variable a = new Variable<>(alias, new SingleProperty<>(entity.clazz, primaryKey.type, primaryKey.field));
                                Variable b = new Variable<>(childRoot.getAlias(), new SingleProperty<>(child.clazz, primaryKey.type, primaryKey.field));

                                Expression onChild = PredicateImpl.eq(db, this, a, b);
                                on = on == null ? onChild : on.and(onChild);
                            }

                            if (on == null)
                                throw new QueryException("Can't merge inherited tables");

                            childRoot.hierarchyVisited.set(true);

                            root = new Join<>(db, Join.JoinType.LEFT, root, childRoot, on);

                            // Depth first scan
                            if (child.children.size() != 0) {
                                children.push(child);
                            }
                        }
                    }
                }

                return root.toString();
            }

            // Tree exploration not needed
            return escape(entity.tableName) + " AS " + escape(getAlias());

        } finally {
            // Reset the status ot allow a second expansion (even in the same query)
            hierarchyVisited.set(false);
        }
    }


    @Override
    public Class<X> getEntityClass() {
        return entity.clazz;
    }


    @NonNull
    @Override
    public String getAlias() {
        return alias;
    }


    @Override
    public <Y> Root<X> join(@NonNull Root<Y> root, @NonNull Property<X, Y> property) {
        return new Join<>(db, Join.JoinType.INNER, this, (RootInt<Y>) root, property);
    }


    @Override
    public <T> Expression isNull(@NonNull SingleProperty<X, T> property) {
        Variable<T> a = new Variable<>(alias, property);

        return PredicateImpl.isNull(db, this, a);
    }


    @Override
    public <T> Expression eq(@NonNull SingleProperty<X, T> property, @Nullable T value) {
        // Just in case the user wants to check for a null property but wrongly calls this
        // method instead of isNull
        if (value == null) {
            return isNull(property);
        }

        Variable<T> a = new Variable<>(alias, property);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.eq(db, this, a, b);
    }


    @Override
    public <T> Expression eq(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.eq(db, this, a, b);
    }


    @Override
    public <T> Expression gt(@NonNull SingleProperty<X, T> property, @NonNull T value) {
        Variable<T> a = new Variable<>(alias, property);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.gt(db, this, a, b);
    }


    @Override
    public <T> Expression gt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.gt(db, this, a, b);
    }


    @Override
    public <T> Expression ge(@NonNull SingleProperty<X, T> property, @NonNull T value) {
        Variable<T> a = new Variable<>(alias, property);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.ge(db, this, a, b);
    }


    @Override
    public <T> Expression ge(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.ge(db, this, a, b);
    }


    @Override
    public <T> Expression lt(@NonNull SingleProperty<X, T> property, @NonNull T value) {
        Variable<T> a = new Variable<>(alias, property);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.lt(db, this, a, b);
    }


    @Override
    public <T> Expression lt(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.lt(db, this, a, b);
    }


    @Override
    public <T> Expression le(@NonNull SingleProperty<X, T> property, @NonNull T value) {
        Variable<T> a = new Variable<>(alias, property);
        Variable<T> b = new Variable<>(value);

        return PredicateImpl.le(db, this, a, b);
    }


    @Override
    public <T> Expression le(@NonNull SingleProperty<X, T> x, @NonNull SingleProperty<X, T> y) {
        Variable<T> a = new Variable<>(alias, x);
        Variable<T> b = new Variable<>(alias, y);

        return PredicateImpl.le(db, this, a, b);
    }


    @Override
    public Map<String, Root<?>> getRootsMap() {
        return Collections.singletonMap(getAlias(), this);
    }

}
