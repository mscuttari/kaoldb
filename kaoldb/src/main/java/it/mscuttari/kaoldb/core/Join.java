package it.mscuttari.kaoldb.core;

import android.support.annotation.NonNull;
import android.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
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


    private JoinType type;
    private From<Y> from;
    private Expression on;


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

        Field field;

        // Get field
        try {
            field = from.getEntityClass().getField(property.getFieldName());
        } catch (NoSuchFieldException e) {
            throw new QueryException("Field " + property.getFieldName() + " not found in entity " + entityClass.getSimpleName());
        }

        List<Pair<String, String>> columnsPairs = decomposeJoinColumn(field, from.getAlias(), alias);

        for (Pair<String, String> pair : columnsPairs) {
            Variable<Y, StringWrapper> a = new Variable<>(new StringWrapper(pair.first));
            Variable<X, StringWrapper> b = new Variable<>(new StringWrapper(pair.second));
            Expression association = PredicateImpl.eq(db, a, b);
            this.on = this.on == null ? association : this.on.and(association);
        }
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
        this.on = on;
    }


    /**
     * Get string representation to be used in query
     *
     * @return  "from" clause
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(").append(from).append(" ").append(type).append(" ").append(super.toString());
        if (on != null) sb.append(" ON ").append(on);
        sb.append(")");

        return sb.toString();
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
     * Get columns pairs to join two entities
     *
     * @param   xField      left side field
     * @param   xAlias      left side table alias
     * @param   yAlias      right side table alias
     *
     * @return  columns pairs
     */
    private static List<Pair<String, String>> decomposeJoinColumn(Field xField, String xAlias, String yAlias) {
        List<Pair<String, String>> result = new ArrayList<>();

        // Joined classes
        Class<?> xClass = xField.getDeclaringClass();
        Class<?> yClass = xField.getType();

        // Fully qualified aliases
        String xFullAlias = xAlias + xClass.getSimpleName();
        String yFullAlias = yAlias + yClass.getSimpleName();

        // @JoinColumn
        if (xField.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn annotation = xField.getAnnotation(JoinColumn.class);
            result.add(new Pair<>(xFullAlias + "." + annotation.name(), yFullAlias + "." + annotation.referencedColumnName()));
            return result;
        }

        // @JoinColumns
        if (xField.isAnnotationPresent(JoinColumns.class)) {
            JoinColumns annotation = xField.getAnnotation(JoinColumns.class);

            for (JoinColumn joinColumn : annotation.value())
                result.add(new Pair<>(xFullAlias + "." + joinColumn.name(), yFullAlias + "." + joinColumn.referencedColumnName()));

            return result;
        }

        // @JoinTable
        if (xField.isAnnotationPresent(JoinTable.class)) {
            // TODO: N:N relationship
            throw new QueryException("Not implemented");
        }

        throw new QueryException("Invalid join field " + xField.getName());
    }


    /**
     * String wrapper class
     * Used in the Variable class creation in order to avoid the quotation marks in the resulting query
     */
    private static class StringWrapper {

        private String value;

        StringWrapper(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

}