package it.mscuttari.kaoldb.core;

import android.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.exceptions.QueryException;

class PredicateImpl extends ExpressionImpl {

    private enum PredicateType {
        EQUAL("="),
        GT(">"),
        GE(">="),
        LT("<"),
        LE("<=");

        private String operation;

        PredicateType(String operation) {
            this.operation = operation;
        }

        @Override
        public String toString() {
            return operation;
        }
    }


    private PredicateType operation;
    private DatabaseObject db;
    private Variable x;
    private Variable y;


    /**
     * Constructor
     *
     * @param   operation   operation
     * @param   db          database object
     * @param   x           first variable
     * @param   y           second variable
     */
    private PredicateImpl(PredicateType operation, DatabaseObject db, Variable x, Variable y) {
        super(null, null, null);

        this.operation = operation;
        this.db = db;
        this.x = x;
        this.y = y;
    }


    /**
     * Create "equals" predicate
     *
     * @param   db  database object
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl eq(DatabaseObject db, Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.EQUAL, db, x, y);
    }


    /**
     * Create "greater than" predicate
     *
     * @param   db  database object
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl gt(DatabaseObject db, Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.GT, db, x, y);
    }


    /**
     * Create "greater or equals than" predicate
     *
     * @param   db  database object
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl ge(DatabaseObject db, Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.GE, db, x, y);
    }


    /**
     * Create "less than" predicate
     *
     * @param   db  database object
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl lt(DatabaseObject db, Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.LT, db, x, y);
    }


    /**
     * Create "less or equals than" predicate
     *
     * @param   db  database object
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl le(DatabaseObject db, Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.LE, db, x, y);
    }


    /**
     * Get string representation to be used in SQL query
     *
     * @return  string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        Object xData = x.getData();
        Object yData = y.getData();

        if (xData instanceof Property && yData instanceof Property) {
            // Two properties
            List<Pair<String, String>> pairs = bindProperties(db, (Property)xData, x.getTableAlias(), (Property)yData, y.getTableAlias());

            String separator = "";
            for (Pair<String, String> pair : pairs) {
                sb.append(separator).append(pair.first).append(operation).append(pair.second);
                separator = " AND ";
            }


        } else if (xData instanceof Property) {
            // Property + value
            List<Pair<String, String>> pairs = bindPropertyObject(db, (Property)xData, x.getTableAlias(), yData);

            String separator = "";
            for (Pair<String, String> pair : pairs) {
                sb.append(separator).append(pair.first).append(operation).append(pair.second);
                separator = " AND ";
            }

        } else {
            // Two values
            sb.append(objectToString(xData));
            sb.append(operation);
            sb.append(objectToString(yData));
        }

        return sb.toString();
    }


    private String objectToString(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else {
            return String.valueOf(value);
        }
    }


    private List<Pair<String, String>> bindProperties(DatabaseObject db, Property xProperty, String xAlias, Property yProperty, String yAlias) {
        List<Pair<String, String>> result = new ArrayList<>(1);
        Field xField, yField;

        // Get fields
        try {
            xField = xProperty.getEntityClass().getField(xProperty.getFieldName());
        } catch (NoSuchFieldException e) {
            throw new QueryException("Field " + xProperty.getFieldName() + " not found in entity " + xProperty.getEntityClass().getSimpleName());
        }

        try {
            yField = yProperty.getEntityClass().getField(yProperty.getFieldName());
        } catch (NoSuchFieldException e) {
            throw new QueryException("Field " + yProperty.getFieldName() + " not found in entity " + yProperty.getEntityClass().getSimpleName());
        }

        if (!xField.getType().isAssignableFrom(yField.getType()) && !yField.getType().isAssignableFrom(xField.getType())) {
            throw new QueryException("Incompatible types");
        }

        // @Column
        if (xField.isAnnotationPresent(Column.class) && yField.isAnnotationPresent(Column.class)) {
            Column xAnnotation = xField.getAnnotation(Column.class);
            Column yAnnotation = yField.getAnnotation(Column.class);

            result.add(new Pair<>(xAlias + "." + xAnnotation.name(), yAlias + "." + yAnnotation.name()));
            return result;
        }

        // @JoinColumn
        if (xField.isAnnotationPresent(JoinColumn.class) && yField.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn xAnnotation = xField.getAnnotation(JoinColumn.class);
            JoinColumn yAnnotation = yField.getAnnotation(JoinColumn.class);

            result.add(new Pair<>(xAlias + "." + xAnnotation.name(), yAlias + "." + yAnnotation.name()));
            return result;
        }

        // @JoinColumns
        if (xField.isAnnotationPresent(JoinColumns.class) && yField.isAnnotationPresent(JoinColumns.class)) {
            JoinColumns xAnnotation = xField.getAnnotation(JoinColumns.class);
            JoinColumns yAnnotation = yField.getAnnotation(JoinColumns.class);

            outer:
            for (JoinColumn xJoinColumn : xAnnotation.value()) {
                for (JoinColumn yJoinColumn : yAnnotation.value()) {
                    if (xJoinColumn.referencedColumnName().equals(yJoinColumn.referencedColumnName())) {
                        result.add(new Pair<>(xAlias + "." + xJoinColumn.name(), yAlias + "." + yJoinColumn.name()));
                        continue outer;
                    }
                }
            }

            return result;
        }

        throw new QueryException("Invalid parameters");
    }


    private List<Pair<String, String>> bindPropertyObject(DatabaseObject db, Property property, String alias, Object obj) {
        List<Pair<String, String>> result = new ArrayList<>(1);
        Field field;

        // Get field
        try {
            field = property.getEntityClass().getField(property.getFieldName());
        } catch (NoSuchFieldException e) {
            throw new QueryException("Field " + property.getFieldName() + " not found in entity " + property.getEntityClass().getSimpleName());
        }

        // Object type must be compatible with the property
        if (!field.getType().isAssignableFrom(obj.getClass()))
            throw new QueryException("Invalid object class");

        // @Column
        if (field.isAnnotationPresent(Column.class)) {
            Column annotation = field.getAnnotation(Column.class);
            result.add(new Pair<>(alias + "." + annotation.name(), objectToString(obj)));
            return result;
        }

        // @JoinColumn
        if (field.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn annotation = field.getAnnotation(JoinColumn.class);
            EntityObject referencedEntity = db.entities.get(field.getType());
            ColumnObject referecedColumn = referencedEntity.columnsNameMap.get(annotation.referencedColumnName());

            try {
                Object value = referecedColumn.field.get(obj);
                result.add(new Pair<>(alias + "." + annotation.name(), objectToString(value)));
            } catch (IllegalAccessException e) {
                throw new QueryException(e.getMessage());
            }
        }

        // @JoinColumns
        if (field.isAnnotationPresent(JoinColumns.class)) {
            JoinColumns annotation = field.getAnnotation(JoinColumns.class);

            for (JoinColumn joinColumn : annotation.value()) {
                EntityObject referencedEntity = db.entities.get(field.getType());
                ColumnObject referecedColumn = referencedEntity.columnsNameMap.get(joinColumn.referencedColumnName());

                try {
                    Object value = referecedColumn.field.get(obj);
                    result.add(new Pair<>(alias + "." + joinColumn.name(), objectToString(value)));
                } catch (IllegalAccessException e) {
                    throw new QueryException(e.getMessage());
                }
            }

            return result;
        }

        return result;
    }

}
