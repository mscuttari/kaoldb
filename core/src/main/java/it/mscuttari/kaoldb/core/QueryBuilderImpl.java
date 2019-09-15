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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.mscuttari.kaoldb.core.StringUtils.escape;

/**
 * QueryBuilder implementation.
 *
 * @see QueryBuilder
 * @param <T>   result objects class
 */
class QueryBuilderImpl<T> implements QueryBuilder<T> {

    @NonNull private final EntityManagerImpl entityManager;
    @NonNull private final DatabaseObject db;
    @NonNull private final Class<T> resultClass;

    // Counter for the roots got from this query builder.
    // It is used to create an unique alias for each root.
    private AtomicInteger rootCounter = new AtomicInteger(0);

    private RootInt<?> from;
    private ExpressionInt where;


    /**
     * Constructor.
     *
     * @param db                database
     * @param resultClass       class of the query result object
     * @param entityManager     entity manager
     */
    QueryBuilderImpl(@NonNull DatabaseObject db,
                     @NonNull Class<T> resultClass,
                     @NonNull EntityManagerImpl entityManager) {

        this.db = db;
        this.resultClass = checkNotNull(resultClass);
        this.entityManager = entityManager;
    }


    @Override
    public <M> Root<M> getRoot(@NonNull Class<M> entityClass) {
        return new From<>(db, this, entityClass, "a" + rootCounter.getAndIncrement());
    }


    @Override
    public QueryBuilder<T> from(Root<?> from) {
        if (!(from instanceof RootInt)) {
            // Security check. Normally not reachable.
            throw new IllegalArgumentException("Incompatible root");
        }

        this.from = (RootInt<?>) from;
        return this;
    }


    @Override
    public QueryBuilder<T> where(Expression where) {
        if (!(where instanceof ExpressionInt)) {
            // Security check. Normally not reachable.
            throw new IllegalArgumentException("Incompatible expression");
        }

        this.where = (ExpressionInt) where;
        return this;
    }


    @Override
    public Query<T> build(@NonNull Root<T> root) {
        // The "FROM" clause must be set
        if (from == null) {
            throw new QueryException("\"FROM\" clause not set");
        }

        if (!(root instanceof RootInt)) {
            // Security check. Normally not reachable.
            throw new IllegalArgumentException("Incompatible root");
        }

        // The requested root must be in the FROM structure
        Map<String, Root<?>> aliasRootMap = from.getRootsMap();

        if (!aliasRootMap.containsKey(((RootInt<T>) root).getAlias())) {
            throw new QueryException("The root doesn't belong to the \"FROM\" structure");
        }

        // Start the real building part
        Root<?> from = createJoinForPredicates(this.from, where);
        String sql = "SELECT " + getSelectClause((RootInt<T>) root) +
                     " FROM " + from;

        if (where != null) {
            sql += " WHERE " + where;
        }

        return new QueryImpl<>(db, entityManager, resultClass, ((RootInt) root).getAlias(), sql);
    }


    /**
     * Create the joins according to the predicates.<br>
     * If for example, an equality predicate is referred to another entity, a join with that
     * entity table is needed.
     *
     * @param root      original root
     * @param where     <code>WHERE</code> clause
     *
     * @return root extended with the required joins
     */
    private Root<?> createJoinForPredicates(Root<?> root, Expression where) {
        // No other entity involved
        if (where == null)
            return root;

        if (!(where instanceof ExpressionInt)) {
            // Security check. Normally not reachable.
            throw new IllegalArgumentException("Incompatible \"WHERE\" expression");
        }

        Map<String, Root<?>> aliasRootMap = ((RootInt<?>) root).getRootsMap();

        // Iterate through the predicates of the expression
        for (PredicateImpl<?> predicate : (ExpressionInt) where) {
            if (!predicate.x.hasProperty())
                continue;

            Property property = predicate.x.getProperty();

            String referencedEntityAlias = predicate.isLeftVariableDerivedFromRoot() ?
                    predicate.root.getAlias() :
                    predicate.root.getAlias() + property.fieldName;

            // Check if the destination root has already been joined by the user
            if (aliasRootMap.containsKey(referencedEntityAlias)) {
                continue;
            }

            // If not, join it
            if (property.columnAnnotation != Column.class) {
                Root<?> joinedRoot = new From<>(db, this, (Class<?>) property.fieldType, referencedEntityAlias);
                root = root.join(joinedRoot, property);
            }
        }

        return root;
    }


    /**
     * Get the <code>SELECT</code> clause to be used in the query.
     *
     * @param root      root
     * @return <code>SELECT</code> clause
     */
    private String getSelectClause(RootInt<T> root) {
        List<String> columns = new ArrayList<>();

        // Determine the root entity.
        // resultClass is actually the same of root.getEntityClass(); the former is preferred to
        // the latter in order to avoid the recursion of root.getEntityClass().
        EntityObject<?> entity = db.getEntity(resultClass);

        // Current entity
        for (BaseColumnObject column : entity.columns) {
            columns.add(
                    escape(root.getAlias()) + "." + escape(column.name) +
                    " AS " +
                    escape(root.getAlias() + "." + column.name)
            );
        }

        // Parents
        EntityObject<?> parent = entity.parent;

        while (parent != null) {
            for (BaseColumnObject column : parent.columns) {
                columns.add(
                        escape(root.getAlias() + parent.getName()) + "." + escape(column.name) +
                        " AS " +
                        escape(root.getAlias() + parent.getName() + "." + column.name)
                );
            }

            parent = parent.parent;
        }

        // Children
        Stack<EntityObject<?>> children = new Stack<>();
        children.push(entity);

        while (!children.empty()) {
            EntityObject<?> node = children.pop();

            for (EntityObject<?> child : node.children) {
                for (BaseColumnObject column : child.columns) {
                    columns.add(escape(root.getAlias() + child.getName()) + "." + escape(column.name) +
                                " AS " +
                                escape(root.getAlias() + child.getName() + "." + column.name)
                    );
                }

                // Depth first scan
                if (child.children.size() != 0) {
                    children.push(child);
                }
            }
        }

        return StringUtils.implode(columns, obj -> obj, ", ");
    }

}
