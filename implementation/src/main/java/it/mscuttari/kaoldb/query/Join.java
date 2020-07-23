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

package it.mscuttari.kaoldb.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.StringUtils;
import it.mscuttari.kaoldb.mapping.DatabaseObject;
import it.mscuttari.kaoldb.mapping.EntityObject;
import it.mscuttari.kaoldb.mapping.Relationship;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

import static it.mscuttari.kaoldb.mapping.Relationship.RelationshipType.MANY_TO_MANY;
import static it.mscuttari.kaoldb.mapping.Relationship.RelationshipType.MANY_TO_ONE;
import static it.mscuttari.kaoldb.mapping.Relationship.RelationshipType.ONE_TO_MANY;
import static it.mscuttari.kaoldb.mapping.Relationship.RelationshipType.ONE_TO_ONE;
import static it.mscuttari.kaoldb.StringUtils.escape;

/**
 * @param <L>   left side entity of the join
 * @param <R>   right side entity of the join
 */
final class Join<L, R> implements Root<L> {

    public enum JoinType {

        INNER("INNER JOIN"),
        LEFT("LEFT JOIN");

        private String clause;

        JoinType(String clause) {
            this.clause = clause;
        }

        @NonNull
        @Override
        public String toString() {
            return clause;
        }

    }

    @NonNull private final DatabaseObject db;
    @NonNull private final JoinType type;
    @NonNull private final Root<L> left;
    @NonNull private final Root<R> right;

    // If the linking property is null then the ON expression is not null and vice versa

    @Nullable private final Property<L, R> property;
    @Nullable private final Expression on;

    // Keep track of the leaf roots in order to avoid the tree spanning when searching for a
    // specific root.

    private final Collection<Root<?>> leaves;

    /**
     * Constructor.
     *
     * @param db            database
     * @param type          join type
     * @param left          left entity root to be joined
     * @param right         right entity root to be joined
     * @param property      first entity property to be used as bridge
     */
    Join(@NonNull DatabaseObject db,
         @NonNull JoinType type,
         @NonNull Root<L> left,
         @NonNull Root<R> right,
         @NonNull Property<L, R> property) {

        this.db = db;
        this.type = type;
        this.left = left;
        this.right = right;
        this.property = property;
        this.on = null;

        Collection<Root<?>> leftRoots = left.getJoinedRoots();
        Collection<Root<?>> rightRoots = right.getJoinedRoots();

        this.leaves = new ArraySet<>(leftRoots.size() + rightRoots.size());

        this.leaves.addAll(leftRoots);
        this.leaves.addAll(rightRoots);
    }

    /**
     * Constructor.
     *
     * @param db        database
     * @param type      join type
     * @param left      left entity root to be joined
     * @param right     right entity root to be joined
     * @param on        custom "ON" expression
     */
    Join(@NonNull DatabaseObject db,
         @NonNull JoinType type,
         @NonNull Root<L> left,
         @NonNull Root<R> right,
         @NonNull Expression on) {

        this.db = db;
        this.type = type;
        this.left = left;
        this.right = right;
        this.property = null;
        this.on = on;

        Collection<Root<?>> leftRoots = left.getJoinedRoots();
        Collection<Root<?>> rightRoots = right.getJoinedRoots();

        this.leaves = new ArraySet<>(leftRoots.size() + rightRoots.size());

        this.leaves.addAll(leftRoots);
        this.leaves.addAll(rightRoots);
    }

    /**
     * Get the <code>FROM</code> clause be used in the query.
     *
     * <p>
     * The join tree is spanned from the most left leaf to to the right most one.
     * This is done because some SQLite implementations does not support, although standard-complaint,
     * "FROM" clauses such as <code>"(table1 INNER JOIN (table2 INNER JOIN table3 ON exp2) ON exp1)"</code>.
     * Therefore, the tree must be seen as a left recursive one, which leads to the equivalent but
     * correctly interpreted clause <code>"((table1 INNER JOIN table2 ON exp1) INNER JOIN table3 ON exp2)"</code>.
     * </p>
     *
     * <p>
     * For example, given the following tree, where<br>
     * nodes 1, 2, 5 are {@link Join} nodes<br>
     * nodes 4, 6, 7, 3 are {@link From} nodes<br>
     * <pre>
     *        1
     *       / \
     *      2   3
     *     / \
     *    4   5
     *       / \
     *      6   7</pre>
     * the visiting order is <code>1 -> 2 -> 4 -> 5 > 6 -> 7 -> 3</code> and the resulting clause is
     * <code>"(((4 join 6) join 7) join 3)"</code>, obviously enriched with the proper
     * <code>ON</code> clauses.
     * </p>
     *
     * @return <code>FROM</code> clause
     */
    @Override
    public String toString() {
        Stack<Join<?, ?>> stack = new Stack<>();

        // Go down to the left-most leaf.
        // That element represents the point of start for the "FROM" clause.
        // In the example: go down to node 4.

        Root<?> root = this;

        while (root instanceof Join) {
            stack.push((Join<?, ?>) root);
            root = ((Join<?, ?>) root).left;
        }

        StringBuilder result = new StringBuilder(root.toString());

        // Move to the right in the tree in order to discover the other leaves

        while (!stack.isEmpty()) {
            Join<?, ?> join = stack.pop();

            if (join.right instanceof From) {
                // The join doesn't have a right subtree, so the join can be performed without further exploration.
                // In the example, the nodes satisfying this condition are the 1 and 5.

                List<String> clauses = getJoinClauses(db, join.type, root, join.right, join.property, join.on);

                for (String clause : clauses) {
                    result.insert(0, "(").append(clause).append(")");
                }

            } else if (join.right instanceof Join) {
                // Subtree found (as in nodes 2 and 5)
                Root<?> rightRoot = join.right;

                // Go down to the left-most leaf of the subtree.
                // In the example, if we were in node 4, we now go to node 5.

                while (rightRoot instanceof Join) {
                    stack.push((Join<?, ?>) rightRoot);
                    rightRoot = ((Join<?, ?>) rightRoot).left;
                }

                // Perform the join (i.e. between 4 and 6)
                List<String> clauses = getJoinClauses(db, join.type, root, rightRoot, join.property, join.on);

                for (String clause : clauses) {
                    result.insert(0, "(").append(clause).append(")");
                }

                // Save the leaf as the new left-most leaf (the bigger tree analysis has already finished)
                root = rightRoot;
            }
        }

        return result.toString();
    }

    @NonNull
    @Override
    public Class<L> getEntityClass() {
        return left.getEntityClass();
    }

    @NonNull
    @Override
    public String getAlias() {
        return left.getAlias();
    }

    @NonNull
    @Override
    public <Y> Root<L> join(@NonNull Root<Y> root, @NonNull Property<L, Y> property) {
        return new Join<>(db, Join.JoinType.INNER, this, root, property);
    }

    @NonNull
    @Override
    public Collection<Root<?>> getJoinedRoots() {
        return Collections.unmodifiableCollection(leaves);
    }

    @NonNull
    @Override
    public <T> Expression isNull(@NonNull SingleProperty<L, T> property) {
        return left.isNull(property);
    }

    @NonNull
    @Override
    public <T> Expression eq(@NonNull SingleProperty<L, T> property, @Nullable T value) {
        return left.eq(property, value);
    }

    @NonNull
    @Override
    public <T> Expression eq(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.eq(x, y);
    }

    @NonNull
    @Override
    public <T> Expression gt(@NonNull SingleProperty<L, T> property, @NonNull T value) {
        return left.gt(property, value);
    }

    @NonNull
    @Override
    public <T> Expression gt(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.gt(x, y);
    }

    @NonNull
    @Override
    public <T> Expression ge(@NonNull SingleProperty<L, T> property, @NonNull T value) {
        return left.ge(property, value);
    }

    @NonNull
    @Override
    public <T> Expression ge(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.ge(x, y);
    }

    @NonNull
    @Override
    public <T> Expression lt(@NonNull SingleProperty<L, T> property, @NonNull T value) {
        return left.lt(property, value);
    }

    @NonNull
    @Override
    public <T> Expression lt(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.lt(x, y);
    }

    @NonNull
    @Override
    public <T> Expression le(@NonNull SingleProperty<L, T> property, @NonNull T value) {
        return left.le(property, value);
    }

    @NonNull
    @Override
    public <T> Expression le(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.le(x, y);
    }

    @NonNull
    @Override
    public <T> Expression between(@NonNull SingleProperty<L, T> property, @NonNull T x, @NonNull T y) {
        return left.between(property, x, y);
    }

    @NonNull
    @Override
    public <T> Expression between(@NonNull SingleProperty<L, T> property, @NonNull SingleProperty<L, T> x, @NonNull T y) {
        return left.between(property, x, y);
    }

    @NonNull
    @Override
    public <T> Expression between(@NonNull SingleProperty<L, T> property, @NonNull T x, @NonNull SingleProperty<L, T> y) {
        return left.between(property, x, y);
    }

    @NonNull
    @Override
    public <T> Expression between(@NonNull SingleProperty<L, T> property, @NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.between(property, x, y);
    }

    @NonNull
    @Override
    public Expression like(@NonNull SingleProperty<L, String> property, @NonNull String value) {
        return left.like(property, value);
    }

    @NonNull
    @Override
    public Expression like(@NonNull SingleProperty<L, String> x, @NonNull SingleProperty<L, String> y) {
        return left.like(x, y);
    }

    @NonNull
    @Override
    public Expression glob(@NonNull SingleProperty<L, String> property, @NonNull String value) {
        return left.glob(property, value);
    }

    @NonNull
    @Override
    public Expression glob(@NonNull SingleProperty<L, String> x, @NonNull SingleProperty<L, String> y) {
        return left.glob(x, y);
    }

    /**
     * Get the join clauses of a join.
     *
     * @param db            database
     * @param type          join type
     * @param local         local root root
     * @param joined        joined root
     * @param property      linking property
     * @param on            <code>ON</code> custom clause
     *
     * @return list of the clauses (in the form <code>" INNER JOIN table ON expression"</code>).
     *         Note that the left table name is not included and must be added by the caller
     *
     * @see Relationship
     */
    private static List<String> getJoinClauses(DatabaseObject db, JoinType type, Root<?> local, Root<?> joined, Property<?, ?> property, Expression on) {
        // Predefined ON clause
        if (on != null) {
            return Collections.singletonList(" " + type + " " + joined.toString() + " ON " + on);
        }

        // Get the relationship linked to the property
        EntityObject<?> leftEntity = db.getEntity(local.getEntityClass());
        Relationship relationship = leftEntity.relationships.get(property.fieldName);

        if (relationship.type == ONE_TO_ONE) {
            if (relationship.owning) {
                if (property.columnAnnotation == JoinColumn.class) {
                    on = getTwoTablesOnClause(db, local, joined, relationship.mappingField.getAnnotation(JoinColumn.class));
                    String result = " " + type + " " + joined.toString() + " ON " + on;
                    return Collections.singletonList(result);
                }

                if (property.columnAnnotation == JoinColumns.class) {
                    on = getTwoTablesOnClause(db, local, joined, relationship.mappingField.getAnnotation(JoinColumns.class));
                    String result = " " + type + " " + joined.toString() + " ON " + on;
                    return Collections.singletonList(result);
                }

                if (property.columnAnnotation == JoinTable.class) {
                    JoinTable annotation = relationship.mappingField.getAnnotation(JoinTable.class);
                    String midTable = annotation.name() + " AS " + getJoinTableAlias(annotation.name(), local.getAlias(), joined.getAlias());

                    Expression onLeft = getThreeTablesDirectOnClause(db, local, joined, annotation);
                    Expression onRight = getThreeTablesInverseOnClause(db, local, joined, annotation);

                    List<String> result = new ArrayList<>(2);

                    result.add(" " + type + " " + midTable + " ON " + onLeft);
                    result.add(" " + type + " " + joined   + " ON " + onRight);

                    return result;
                }
            } else {
                if (relationship.mappingField.isAnnotationPresent(JoinColumn.class)) {
                    on = getTwoTablesOnClause(db, joined, local, relationship.mappingField.getAnnotation(JoinColumn.class));
                    String result = " " + type + " " + joined.toString() + " ON " + on;
                    return Collections.singletonList(result);
                }

                if (relationship.mappingField.isAnnotationPresent(JoinColumns.class)) {
                    on = getTwoTablesOnClause(db, joined, local, relationship.mappingField.getAnnotation(JoinColumns.class));
                    String result = " " + type + " " + joined.toString() + " ON " + on;
                    return Collections.singletonList(result);
                }

                if (relationship.mappingField.isAnnotationPresent(JoinTable.class)) {
                    JoinTable annotation = relationship.mappingField.getAnnotation(JoinTable.class);
                    String midTable = annotation.name() + " AS " + getJoinTableAlias(annotation.name(), local.getAlias(), joined.getAlias());

                    Expression onLeft = getThreeTablesInverseOnClause(db, local, joined, annotation);
                    Expression onRight = getThreeTablesDirectOnClause(db, local, joined, annotation);

                    List<String> result = new ArrayList<>(2);

                    result.add(" " + type + " " + midTable + " ON " + onLeft);
                    result.add(" " + type + " " + joined   + " ON " + onRight);

                    return result;
                }
            }
        }

        if (relationship.type == ONE_TO_MANY) {
            if (relationship.mappingField.isAnnotationPresent(JoinColumn.class)) {
                on = getTwoTablesOnClause(db, joined, local, relationship.mappingField.getAnnotation(JoinColumn.class));
                String result = " " + type + " " + joined.toString() + " ON " + on;
                return Collections.singletonList(result);
            }

            if (relationship.mappingField.isAnnotationPresent(JoinColumns.class)) {
                on = getTwoTablesOnClause(db, joined, local, relationship.mappingField.getAnnotation(JoinColumns.class));
                String result = " " + type + " " + joined.toString() + " ON " + on;
                return Collections.singletonList(result);
            }

            if (relationship.mappingField.isAnnotationPresent(JoinTable.class)) {
                JoinTable annotation = relationship.mappingField.getAnnotation(JoinTable.class);
                String midTable = annotation.name() + " AS " + getJoinTableAlias(annotation.name(), local.getAlias(), joined.getAlias());

                Expression onLeft = getThreeTablesInverseOnClause(db, local, joined, annotation);
                Expression onRight = getThreeTablesDirectOnClause(db, local, joined, annotation);

                List<String> result = new ArrayList<>(2);

                result.add(" " + type + " " + midTable + " ON " + onLeft);
                result.add(" " + type + " " + joined   + " ON " + onRight);

                return result;
            }
        }

        if (relationship.type == MANY_TO_ONE) {
            if (property.columnAnnotation == JoinColumn.class) {
                on = getTwoTablesOnClause(db, local, joined, relationship.mappingField.getAnnotation(JoinColumn.class));
                String result = " " + type + " " + joined.toString() + " ON " + on;
                return Collections.singletonList(result);
            }

            if (property.columnAnnotation == JoinColumns.class) {
                on = getTwoTablesOnClause(db, local, joined, relationship.mappingField.getAnnotation(JoinColumns.class));
                String result = " " + type + " " + joined.toString() + " ON " + on;
                return Collections.singletonList(result);
            }

            if (property.columnAnnotation == JoinTable.class) {
                JoinTable annotation = relationship.mappingField.getAnnotation(JoinTable.class);
                String midTable = annotation.name() + " AS " + getJoinTableAlias(annotation.name(), local.getAlias(), joined.getAlias());

                Expression onLeft = getThreeTablesDirectOnClause(db, local, joined, annotation);
                Expression onRight = getThreeTablesInverseOnClause(db, local, joined, annotation);

                List<String> result = new ArrayList<>(2);

                result.add(" " + type + " " + midTable + " ON " + onLeft);
                result.add(" " + type + " " + joined   + " ON " + onRight);

                return result;
            }
        }

        if (relationship.type == MANY_TO_MANY) {
            if (relationship.owning) {
                JoinTable annotation = relationship.mappingField.getAnnotation(JoinTable.class);
                String midTable = annotation.name() + " AS " + getJoinTableAlias(annotation.name(), local.getAlias(), joined.getAlias());

                Expression onLeft = getThreeTablesDirectOnClause(db, local, joined, annotation);
                Expression onRight = getThreeTablesInverseOnClause(db, local, joined, annotation);

                List<String> result = new ArrayList<>(2);

                result.add(" " + type + " " + midTable + " ON " + onLeft);
                result.add(" " + type + " " + joined   + " ON " + onRight);

                return result;

            } else {
                JoinTable annotation = relationship.mappingField.getAnnotation(JoinTable.class);
                String midTable = annotation.name() + " AS " + getJoinTableAlias(annotation.name(), local.getAlias(), joined.getAlias());

                Expression onLeft = getThreeTablesInverseOnClause(db, local, joined, annotation);
                Expression onRight = getThreeTablesDirectOnClause(db, local, joined, annotation);

                List<String> result = new ArrayList<>(2);

                result.add(" " + type + " " + midTable + " ON " + onLeft);
                result.add(" " + type + " " + joined   + " ON " + onRight);

                return result;
            }
        }

        // Normally not reachable
        throw new QueryException("Invalid join field \"" + property.fieldName + "\"");
    }

    /**
     * Get the <code>ON</code> expression for the direct join columns of a {@link JoinColumn}
     * annotated property.
     *
     * @param db            database
     * @param owning        owning root
     * @param referenced    referenced (non-owning) root
     * @param annotation    JoinColumn annotation
     *
     * @return "ON" expression
     */
    private static Expression getTwoTablesOnClause(DatabaseObject db, Root<?> owning, Root<?> referenced, JoinColumn annotation) {
        return getColumnsEqualityExpression(
                db,
                owning,
                escape(owning.getAlias())     + "." + escape(annotation.name()),
                escape(referenced.getAlias()) + "." + escape(annotation.referencedColumnName())
        );
    }

    /**
     * Get the "ON" expression for the join columns of a {@link JoinColumns} annotated property.
     *
     * @param db            database
     * @param owning        owning root
     * @param referenced    referenced (non-owning) root
     * @param annotation    JoinColumns annotation
     *
     * @return "ON" expression
     */
    private static Expression getTwoTablesOnClause(DatabaseObject db, Root<?> owning, Root<?> referenced, JoinColumns annotation) {
        Expression result = null;

        for (JoinColumn joinColumn : annotation.value()) {
            Expression expression = getTwoTablesOnClause(db, owning, referenced, joinColumn);
            result = result == null ? expression : result.and(expression);
        }

        return result;
    }

    /**
     * Get the "ON" expression for the direct join columns of a {@link JoinTable} annotated property.
     *
     * @param db            database
     * @param direct        direct join root
     * @param inverse       inverse join root
     * @param annotation    JoinTable annotation
     *
     * @return "ON" expression
     */
    private static Expression getThreeTablesDirectOnClause(DatabaseObject db, Root<?> direct, Root<?> inverse, JoinTable annotation) {
        String joinTableAlias = getJoinTableAlias(annotation.name(), direct.getAlias(), inverse.getAlias());
        Expression result = null;

        for (JoinColumn joinColumn : annotation.joinColumns()) {
            Expression expression = getColumnsEqualityExpression(
                    db,
                    direct,
                    escape(direct.getAlias()) + "." + escape(joinColumn.referencedColumnName()),
                    escape(joinTableAlias) + "." + escape(joinColumn.name())
            );

            result = result == null ? expression : result.and(expression);
        }

        return result;
    }

    /**
     * Get the "ON" expression for the inverse join columns of a {@link JoinTable} annotated property.
     *
     * @param db            database
     * @param direct        direct join root
     * @param inverse       inverse join root
     * @param annotation    JoinTable annotation
     *
     * @return "ON" expression
     */
    private static Expression getThreeTablesInverseOnClause(DatabaseObject db, Root<?> direct, Root<?> inverse, JoinTable annotation) {
        String joinTableAlias = getJoinTableAlias(annotation.name(), direct.getAlias(), inverse.getAlias());
        Expression result = null;

        for (JoinColumn joinColumn : annotation.inverseJoinColumns()) {
            Expression expression = getColumnsEqualityExpression(
                    db,
                    inverse,
                    escape(inverse.getAlias()) + "." + escape(joinColumn.referencedColumnName()),
                    escape(joinTableAlias) + "." + escape(joinColumn.name())
            );

            result = result == null ? expression : result.and(expression);
        }

        return result;
    }

    /**
     * Given two column names, get an equality expression between them.
     *
     * @param db            database
     * @param firstColumn   first column, prepended with table alias (i.e. <code>"alias.column"</code>)
     * @param secondColumn  second column, prepended with table alias (i.e. <code>"alias.column"</code>)
     *
     * @return equality expression
     */
    private static Expression getColumnsEqualityExpression(DatabaseObject db, Root<?> root, String firstColumn, String secondColumn) {
        Variable<StringUtils.EscapedString> a = new Variable<>(new StringUtils.EscapedString(firstColumn));
        Variable<StringUtils.EscapedString> b = new Variable<>(new StringUtils.EscapedString(secondColumn));

        return PredicateImpl.eq(db, root, a, b);
    }

    /**
     * Get the alias to be used for the join table.
     *
     * @param tableName     join table name
     * @param xAlias        first table alias
     * @param yAlias        second table alias
     *
     * @return join table alias
     */
    private static String getJoinTableAlias(String tableName, String xAlias, String yAlias) {
        return tableName + "X" + xAlias + "Y" + yAlias;
    }

}
