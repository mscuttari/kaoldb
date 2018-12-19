package it.mscuttari.kaoldb.core;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.core.Variable.StringWrapper;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

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

        @Override
        public String toString() {
            return clause;
        }

    }


    @NonNull  private final DatabaseObject db;
    @NonNull  private final JoinType type;
    @NonNull  private final Root<L> left;
    @NonNull  private final Root<R> right;

    // If the linking property is null then the ON expression is not null and vice versa
    @Nullable private final Property<L, R> property;
    @Nullable private final Expression on;


    /**
     * Constructor
     *
     * @param db            database
     * @param type          join type
     * @param left          left entity root to be joined
     * @param right         right entity root to be joined
     * @param property      first entity property to be used as bridge
     *
     * @see JoinType for the possible join types
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
    }


    /**
     * Constructor
     *
     * @param db        database
     * @param type      join type
     * @param left      left entity root to be joined
     * @param right     right entity root to be joined
     * @param on        custom "ON" expression
     *
     * @see JoinType for the possible join types
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
    }


    /**
     * Get the "FROM" clause be used in the query
     *
     * The join tree is spanned from the most left leaf to to the right most one.
     * This is done because some SQLite implementations does not support, although standard
     * complaint, "from" clauses such as "(table1 INNER JOIN (table2 INNER JOIN table3 ON exp2) ON exp1)".
     * Therefore, the tree must be seen as left recursive one, which lead to the equivalent but
     * correctly interpreted clause "((table1 INNER JOIN table2 ON exp1) INNER JOIN table3 ON exp2)".
     *
     * @return "FROM" clause
     */
    @Override
    public String toString() {
        Stack<Join<?, ?>> stack = new Stack<>();

        // Go down to the left-most leaf
        // That element represents the point of start for the "FROM" clause

        Root<?> root = this;

        while (root instanceof Join<?, ?>) {
            stack.push((Join<?, ?>) root);
            root = ((Join<?, ?>) root).left;
        }

        StringBuilder result = new StringBuilder(root.toString());

        // Move to the right in the tree in order to discover the other leaves

        while (!stack.isEmpty()) {
            Join<?, ?> join = stack.pop();

            if (join.right instanceof From<?>) {
                // The join doesn't have subtrees, so the join can be performed without further exploration
                List<Pair<String, Expression>> clauses = getJoinClauses(db, join.type, root, join.right, join.property, join.on);

                for (Pair<String, Expression> clause : clauses) {
                    result = new StringBuilder("(" + result + clause.first + " ON " + clause.second + ")");
                }

            } else if (join.right instanceof Join<?, ?>) {
                // Subtree found
                Root<?> rightRoot = join.right;

                // Go down to the left-most leaf of the subtree
                while (rightRoot instanceof Join) {
                    stack.push((Join<?, ?>) rightRoot);
                    rightRoot = ((Join<?, ?>) rightRoot).left;
                }

                // Perform the join
                List<Pair<String, Expression>> clauses = getJoinClauses(db, join.type, root, rightRoot, join.property, join.on);

                for (Pair<String, Expression> clause : clauses) {
                    result = new StringBuilder("(" + result + clause.first + " ON " + clause.second + ")");
                }

                // Save the leaf as the new left-most leaf (the bigger tree analysis has already finished)
                root = rightRoot;
            }
        }

        return result.toString();
    }


    /**
     * Get left root
     *
     * @return left root
     */
    public Root<L> getLeftRoot() {
        return left;
    }


    /**
     * Get right root
     *
     * @return right root
     */
    public Root<R> getRightRoot() {
        return right;
    }


    @Override
    public Class<L> getEntityClass() {
        return left.getEntityClass();
    }


    @Override
    public String getAlias() {
        return left.getAlias();
    }


    @Override
    public <Y> Root<L> join(@NonNull Root<Y> root, @NonNull Property<L, Y> property) {
        return new Join<>(db, Join.JoinType.INNER, this, root, property);
    }


    @Override
    public Expression isNull(@NonNull SingleProperty<L, ?> field) {
        return left.isNull(field);
    }


    @Override
    public <T> Expression eq(@NonNull SingleProperty<L, T> field, @NonNull T value) {
        return left.eq(field, value);
    }


    @Override
    public <T> Expression eq(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.eq(x, y);
    }


    @Override
    public <Y, T> Expression eq(@NonNull SingleProperty<L, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        return left.eq(x, yClass, yAlias, y);
    }


    @Override
    public <T> Expression gt(@NonNull SingleProperty<L, T> field, @NonNull T value) {
        return left.gt(field, value);
    }


    @Override
    public <T> Expression gt(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.gt(x, y);
    }


    @Override
    public <Y, T> Expression gt(@NonNull SingleProperty<L, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        return left.gt(x, yClass, yAlias, y);
    }


    @Override
    public <T> Expression ge(@NonNull SingleProperty<L, T> field, @NonNull T value) {
        return left.ge(field, value);
    }


    @Override
    public <T> Expression ge(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.ge(x, y);
    }


    @Override
    public <Y, T> Expression ge(@NonNull SingleProperty<L, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        return left.ge(x, yClass, yAlias, y);
    }


    @Override
    public <T> Expression lt(@NonNull SingleProperty<L, T> field, @NonNull T value) {
        return left.lt(field, value);
    }


    @Override
    public <T> Expression lt(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.lt(x, y);
    }


    @Override
    public <Y, T> Expression lt(@NonNull SingleProperty<L, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        return left.lt(x, yClass, yAlias, y);
    }


    @Override
    public <T> Expression le(@NonNull SingleProperty<L, T> field, @NonNull T value) {
        return left.le(field, value);
    }


    @Override
    public <T> Expression le(@NonNull SingleProperty<L, T> x, @NonNull SingleProperty<L, T> y) {
        return left.le(x, y);
    }


    @Override
    public <Y, T> Expression le(@NonNull SingleProperty<L, T> x, @NonNull Class<Y> yClass, @NonNull String yAlias, @NonNull SingleProperty<Y, T> y) {
        return left.le(x, yClass, yAlias, y);
    }


    /**
     * Get the join clauses of a join
     *
     * @param db        database
     * @param type      join type
     * @param left      left root
     * @param right     right root
     * @param property  linking property
     * @param on        "ON" custom clause
     *
     * @return list of the clauses (each clause is composed by two elements: the first is something
     *         like " INNER JOIN table", while the second is the "ON" expression)
     */
    private static List<Pair<String, Expression>> getJoinClauses(DatabaseObject db, JoinType type, Root<?> left, Root<?> right, Property<?, ?> property, Expression on) {
        // Predefined ON clause
        if (on != null) {
            return Collections.singletonList(new Pair<>(
                    " " + type + " " + right.toString(),
                    on)
            );
        }

        // ON clause dependent on the property

        if (property.columnAnnotation == JoinColumn.class) {
            return Collections.singletonList(new Pair<>(
                    " " + type + " " + right.toString(),
                    getTwoTablesOnClause(db, left, right, property.getField().getAnnotation(JoinColumn.class))
            ));

        } else if (property.columnAnnotation == JoinColumns.class) {
            return Collections.singletonList(new Pair<>(
                    " " + type + " " + right.toString(),
                    getTwoTablesOnClause(db, left, right, property.getField().getAnnotation(JoinColumns.class))
            ));

        } else if (property.columnAnnotation == JoinTable.class) {
            JoinTable annotation = property.getField().getAnnotation(JoinTable.class);
            String midTable = annotation.name() + " AS " + getJoinTableAlias(annotation.name(), left.getAlias(), right.getAlias());

            Expression onLeft = getThreeTablesLeftOnClause(db, left, right, annotation);
            Expression onRight = getThreeTablesRightOnClause(db, left, right, annotation);

            List<Pair<String, Expression>> result = new ArrayList<>(2);

            result.add(new Pair<>(
                    " " + type + " " + midTable,
                    onLeft
            ));

            result.add(new Pair<>(
                    " " + type + " " + right,
                    onRight
            ));

            return result;
        }

        throw new QueryException("Invalid join field \"" + property.fieldName + "\"");
    }


    /**
     * Get the "ON" expression for the direct join columns of a {@link JoinColumn} annotated property
     *
     * @param db            database
     * @param left          left root
     * @param right         right root
     * @param annotation    JoinColumn annotation
     *
     * @return "ON" expression
     */
    private static Expression getTwoTablesOnClause(DatabaseObject db, Root<?> left, Root<?> right, JoinColumn annotation) {
        Pair<String, String> columnsPair = new Pair<>(
                left.getAlias()  + "." + annotation.name(),
                right.getAlias() + "." + annotation.referencedColumnName()
        );

        return columnsPairToExpression(db, left, columnsPair);
    }


    /**
     * Get the "ON" expression for the direct join columns of a {@link JoinColumns} annotated property
     *
     * @param db            database
     * @param left          left root
     * @param right         right root
     * @param annotation    JoinColumns annotation
     *
     * @return "ON" expression
     */
    private static Expression getTwoTablesOnClause(DatabaseObject db, Root<?> left, Root<?> right, JoinColumns annotation) {
        Expression result = null;

        for (JoinColumn joinColumn : annotation.value()) {
            Expression expression = getTwoTablesOnClause(db, left, right, joinColumn);
            result = result == null ? expression : result.and(expression);
        }

        return result;
    }


    /**
     * Get the "ON" expression for the direct join columns of a {@link JoinTable} annotated property
     *
     * @param db            database
     * @param left          left root
     * @param right         right root
     * @param annotation    JoinTable annotation
     *
     * @return "ON" expression
     */
    private static Expression getThreeTablesLeftOnClause(DatabaseObject db, Root<?> left, Root<?> right, JoinTable annotation) {
        String joinTableAlias = getJoinTableAlias(annotation.name(), left.getAlias(), right.getAlias());
        Expression result = null;

        for (JoinColumn joinColumn : annotation.joinColumns()) {
            Pair<String, String> columnsPair = new Pair<>(
                    left.getAlias() + "." + joinColumn.referencedColumnName(),
                    joinTableAlias + "." + joinColumn.name()
            );

            Expression expression = columnsPairToExpression(db, left, columnsPair);
            result = result == null ? expression : result.and(expression);
        }

        return result;
    }


    /**
     * Get the "ON" expression for the inverse join columns of a {@link JoinTable} annotated property
     *
     * @param db            database
     * @param left          left root
     * @param right         right root
     * @param annotation    JoinTable annotation
     *
     * @return "ON" expression
     */
    private static Expression getThreeTablesRightOnClause(DatabaseObject db, Root<?> left, Root<?> right, JoinTable annotation) {
        String joinTableAlias = getJoinTableAlias(annotation.name(), left.getAlias(), right.getAlias());
        Expression result = null;

        for (JoinColumn joinColumn : annotation.inverseJoinColumns()) {
            Pair<String, String> columnsPair = new Pair<>(
                    right.getAlias() + "." + joinColumn.referencedColumnName(),
                    joinTableAlias + "." + joinColumn.name()
            );

            Expression expression = columnsPairToExpression(db, right, columnsPair);
            result = result == null ? expression : result.and(expression);
        }

        return result;
    }


    /**
     * Convert a column association pair to the equivalent expression
     *
     * @param db        database
     * @param pair      column association
     *
     * @return expression
     */
    private static Expression columnsPairToExpression(DatabaseObject db, Root<?> root, Pair<String, String> pair) {
        Variable<StringWrapper> a = new Variable<>(new StringWrapper(pair.first));
        Variable<StringWrapper> b = new Variable<>(new StringWrapper(pair.second));

        return PredicateImpl.eq(db, root, a, b);
    }


    /**
     * Get the alias to be used for the join table
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
