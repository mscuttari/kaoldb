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


    @NonNull private final DatabaseObject db;
    @NonNull private final JoinType type;
    @NonNull private final Root<L> left;
    @NonNull private final Root<R> right;

    @Nullable private final Property<L, R> property;
    @Nullable private final Expression on;


    /**
     * Constructor
     *
     * @param db            database object
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
     * @param db        database object
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
    }


    /**
     * Get string representation to be used in the query
     *
     * @return "FROM" clause
     */
    @Override
    public String toString() {
        Stack<Join<?, ?>> stack = new Stack<>();
        List<Pair<String, Expression>> clauses = new ArrayList<>();

        // Go down to the left-most table
        // That element represents the point of start for the "FROM" clause

        Root<?> root = this;

        while (root instanceof Join<?, ?>) {
            stack.push((Join<?, ?>) root);
            root = ((Join<?, ?>) root).left;
        }

        clauses.add(new Pair<>(root.toString(), null));

        // Move to the right in the tree in order to discover the other tables

        while (!stack.isEmpty()) {
            Join<?, ?> join = stack.pop();

            if (join.right instanceof From<?>) {
                clauses.addAll(getJoinClauses(db, join.type, root, join.right, join.property, join.on));

            } else if (join.right instanceof Join) {
                Root<?> rightRoot = join.right;

                while (rightRoot instanceof Join) {
                    stack.push((Join<?, ?>) rightRoot);
                    rightRoot = ((Join<?, ?>) rightRoot).left;
                }

                clauses.addAll(getJoinClauses(db, join.type, root, rightRoot, join.property, join.on));
                root = rightRoot;
            }
        }

        StringBuilder result = new StringBuilder();

        for (Pair<String, Expression> clause : clauses) {
            result = new StringBuilder("(" + result + clause.first);

            if (clause.second != null) {
                result.append(" ON ").append(clause.second);
            }

            result.append(")");
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
    public Class<?> getEntityClass() {
        return left.getEntityClass();
    }


    @Override
    public String getAlias() {
        return left.getAlias();
    }


    @Override
    public String getFullAlias() {
        return left.getFullAlias();
    }


    @Override
    public <Y> Root<L> join(Root<Y> root, Property<L, Y> property) {
        return new Join<>(db, Join.JoinType.INNER, this, root, property);
    }


    @Override
    public Expression isNull(SingleProperty<L, ?> field) {
        return left.isNull(field);
    }


    @Override
    public <T> Expression eq(SingleProperty<L, T> field, T value) {
        return left.eq(field, value);
    }


    @Override
    public <T> Expression eq(SingleProperty<L, T> x, SingleProperty<L, T> y) {
        return left.eq(x, y);
    }


    @Override
    public <Y, T> Expression eq(SingleProperty<L, T> x, Class<Y> yClass, String yAlias, SingleProperty<Y, T> y) {
        return left.eq(x, yClass, yAlias, y);
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
    private static List<Pair<String, Expression>> getJoinClauses(@NonNull DatabaseObject db,
                                                                 @NonNull JoinType type,
                                                                 @NonNull Root<?> left,
                                                                 @NonNull Root<?> right,
                                                                 @Nullable Property<?, ?> property,
                                                                 @Nullable Expression on) {

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
            Expression onRight = getThreeTablesRightOnClause(db, left, right, property, annotation);

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
    private static Expression getTwoTablesOnClause(@NonNull DatabaseObject db,
                                                   @NonNull Root<?> left,
                                                   @NonNull Root<?> right,
                                                   @NonNull JoinColumn annotation) {

        Pair<String, String> columnsPair = new Pair<>(
                left.getFullAlias()  + "." + annotation.name(),
                right.getFullAlias() + "." + annotation.referencedColumnName()
        );

        return columnsPairToExpression(db, columnsPair);
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
    private static Expression getTwoTablesOnClause(@NonNull DatabaseObject db,
                                                   @NonNull Root<?> left,
                                                   @NonNull Root<?> right,
                                                   @NonNull JoinColumns annotation) {

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
    private static Expression getThreeTablesLeftOnClause(@NonNull DatabaseObject db,
                                                         @NonNull Root<?> left,
                                                         @NonNull Root<?> right,
                                                         @NonNull JoinTable annotation) {

        String joinTableAlias = getJoinTableAlias(annotation.name(), left.getAlias(), right.getAlias());
        Expression result = null;

        for (JoinColumn joinColumn : annotation.joinColumns()) {
            Pair<String, String> columnsPair = new Pair<>(
                    left.getFullAlias() + "." + joinColumn.referencedColumnName(),
                    joinTableAlias + "." + joinColumn.name()
            );

            Expression expression = columnsPairToExpression(db, columnsPair);
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
     * @param property      linking property
     * @param annotation    JoinTable annotation
     *
     * @return "ON" expression
     */
    private static Expression getThreeTablesRightOnClause(@NonNull DatabaseObject db,
                                                          @NonNull Root<?> left,
                                                          @NonNull Root<?> right,
                                                          @NonNull Property<?, ?> property,
                                                          @NonNull JoinTable annotation) {

        String joinTableAlias = getJoinTableAlias(annotation.name(), left.getAlias(), right.getAlias());
        String fullAlias = From.getFullAlias(left.getAlias(), property.fieldType);
        Expression result = null;

        for (JoinColumn joinColumn : annotation.inverseJoinColumns()) {
            Pair<String, String> columnsPair = new Pair<>(fullAlias + "." + joinColumn.referencedColumnName(), joinTableAlias + "." + joinColumn.name());
            Expression expression = columnsPairToExpression(db, columnsPair);
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
    private static Expression columnsPairToExpression(@NonNull DatabaseObject db, @NonNull Pair<String, String> pair) {
        Variable<?, StringWrapper> a = new Variable<>(new StringWrapper(pair.first));
        Variable<?, StringWrapper> b = new Variable<>(new StringWrapper(pair.second));

        return PredicateImpl.eq(db, a, b);
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


    /**
     * Get full alias for an automatically joined table
     *
     * @param alias         left alias
     * @param leftClass     left class
     * @param rightClass    right class
     *
     * @return full alias
     */
    public static String getJoinFullAlias(String alias, Class<?> leftClass, Class<?> rightClass) {
        return alias +
                (leftClass == null ? "" : leftClass.getSimpleName()) +
                "Join" +
                (rightClass == null ? "" : rightClass.getSimpleName());
    }

}
