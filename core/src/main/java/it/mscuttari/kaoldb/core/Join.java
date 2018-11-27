package it.mscuttari.kaoldb.core;

import android.util.Pair;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.core.Variable.StringWrapper;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;

/**
 * @param   <X>     right side entity of the join
 * @param   <Y>     left side entity of the join
 */
abstract class Join<X, Y> extends From<X> {

    enum JoinType {
        INNER("INNER JOIN"),
        LEFT("LEFT JOIN"),
        NATURAL("NATURAL JOIN");

        private String clause;

        JoinType(String clause) {
            this.clause = clause;
        }

        @Override
        public String toString() {
            return clause;
        }
    }


    private final JoinType type;
    private final From<Y> from;
    private final String alias;
    private final Property<Y, X> property;
    private final Expression on;


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   type            join type
     * @param   from            first entity root to be joined
     * @param   entityClass     second entity class tot be joined
     * @param   alias           second joined entity alias
     * @param   property        first entity property to be used as bridge
     *
     * @throws  QueryException  if the property field is not found in the first entity class
     */
    Join(DatabaseObject db, @NonNull JoinType type, From<Y> from, Class<X> entityClass, String alias, Property<Y, X> property) {
        super(db, entityClass, alias);

        this.type = type;
        this.from = from;
        this.alias = alias;
        this.property = property;
        this.on = null;
    }


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   type            join type
     * @param   from            first entity root to be joined
     * @param   entityClass     second entity class to be joined
     * @param   alias           second joined entity alias
     * @param   on              "on" expression
     */
    Join(DatabaseObject db, @NonNull JoinType type, From<Y> from, Class<X> entityClass, String alias, Expression on) {
        super(db, entityClass, alias);

        this.type = type;
        this.from = from;
        this.alias = alias;
        this.property = null;
        this.on = on;
    }


    /**
     * Get string representation to be used in query
     *
     * @return  "FROM" clause
     */
    @Override
    public String toString() {
        // Predefined ON clause
        if (on != null) {
            return "(" + from + " " + type + " " + super.toString() + " ON " + on + ")";
        }

        // ON clause dependent on the property
        if (property.columnAnnotation == JoinColumn.class) {
            // Two tables join
            Expression on = getTwoTablesOnClause(property.getField().getAnnotation(JoinColumn.class));
            return "(" + from + " " + type + " " + super.toString() + " ON " + on + ")";

        } else if (property.columnAnnotation == JoinColumns.class) {
            // Two tables join
            Expression on = getTwoTablesOnClause(property.getField().getAnnotation(JoinColumns.class));
            return "(" + from + " " + type + " " + super.toString() + " ON " + on + ")";

        } else if (property.columnAnnotation == JoinTable.class) {
            // Three tables join
            JoinTable annotation = property.getField().getAnnotation(JoinTable.class);
            String midTable = annotation.name() + " AS " + getJoinTableAlias(annotation.name(), from.getAlias(), alias);

            Expression onLeft = getThreeTablesLeftOnClause(annotation);
            Expression onRight = getThreeTablesRightOnClause(annotation);

            return "((" + from + " " + type + " " + midTable + " ON " + onLeft + ") " +
                    type + " " + super.toString() + " ON " + onRight + ")";
        }

        throw new QueryException("Invalid join field \"" + property.fieldName + "\"");
    }


    /**
     * Get left side root of the join
     *
     * @return  root
     */
    public From<Y> getLeftSideRoot() {
        return from;
    }


    /**
     * Get the "ON" expression for the direct join columns of a {@link JoinColumn} annotated property
     *
     * @param   annotation      annotation
     * @return  "ON" expression
     */
    private Expression getTwoTablesOnClause(JoinColumn annotation) {
        String xFullAlias = from.getFullAlias();
        String yFullAlias = getFullAlias();

        Pair<String, String> columnsPair = new Pair<>(xFullAlias + "." + annotation.name(), yFullAlias + "." + annotation.referencedColumnName());
        return columnsPairToExpression(columnsPair);
    }


    /**
     * Get the "ON" expression for the direct join columns of a {@link JoinColumns} annotated property
     *
     * @param   annotation      annotation
     * @return  "ON" expression
     */
    private Expression getTwoTablesOnClause(JoinColumns annotation) {
        Expression result = null;

        for (JoinColumn joinColumn : annotation.value()) {
            Expression expression = getTwoTablesOnClause(joinColumn);
            result = result == null ? expression : result.and(expression);
        }

        return result;
    }


    /**
     * Get the "ON" expression for the direct join columns of a {@link JoinTable} annotated property
     *
     * @param   annotation      annotation
     * @return  "ON" expression
     */
    private Expression getThreeTablesLeftOnClause(JoinTable annotation) {
        String joinTableAlias = getJoinTableAlias(annotation.name(), from.getAlias(), alias);
        String fullAlias = from.getFullAlias();
        Expression result = null;

        for (JoinColumn joinColumn : annotation.joinColumns()) {
            Pair<String, String> columnsPair = new Pair<>(fullAlias + "." + joinColumn.referencedColumnName(), joinTableAlias + "." + joinColumn.name());
            Expression expression = columnsPairToExpression(columnsPair);
            result = result == null ? expression : result.and(expression);
        }

        return result;
    }


    /**
     * Get the "ON" expression for the inverse join columns of a {@link JoinTable} annotated property
     *
     * @param   annotation      annotation
     * @return  "ON" expression
     */
    private Expression getThreeTablesRightOnClause(JoinTable annotation) {
        String joinTableAlias = getJoinTableAlias(annotation.name(), from.getAlias(), alias);
        String fullAlias = getFullAlias(alias, property.fieldType);
        Expression result = null;

        for (JoinColumn joinColumn : annotation.inverseJoinColumns()) {
            Pair<String, String> columnsPair = new Pair<>(fullAlias + "." + joinColumn.referencedColumnName(), joinTableAlias + "." + joinColumn.name());
            Expression expression = columnsPairToExpression(columnsPair);
            result = result == null ? expression : result.and(expression);
        }

        return result;
    }


    /**
     * Convert a column association pair to the equivalent expression
     *
     * @param   pair    column association
     * @return  expression
     */
    private Expression columnsPairToExpression(Pair<String, String> pair) {
        Variable<Y, StringWrapper> a = new Variable<>(new StringWrapper(pair.first));
        Variable<X, StringWrapper> b = new Variable<>(new StringWrapper(pair.second));
        return PredicateImpl.eq(db, a, b);
    }


    /**
     * Get the alias to be used for the join table
     *
     * @param   tableName   join table name
     * @param   xAlias      first table alias
     * @param   yAlias      second table alias
     *
     * @return  join table alias
     */
    private static String getJoinTableAlias(String tableName, String xAlias, String yAlias) {
        return tableName + "X" + xAlias + "Y" + yAlias;
    }


    /**
     * Get full alias for an automatically joined table
     *
     * @param   alias           left alias
     * @param   leftClass       left class
     * @param   rightClass      right class
     *
     * @return  full alias
     */
    public static String getJoinFullAlias(String alias, Class<?> leftClass, Class<?> rightClass) {
        return alias +
                (leftClass == null ? "" : leftClass.getSimpleName()) +
                "Join" +
                (rightClass == null ? "" : rightClass.getSimpleName());
    }

}
