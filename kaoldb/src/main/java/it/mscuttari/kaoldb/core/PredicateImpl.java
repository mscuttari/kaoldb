package it.mscuttari.kaoldb.core;

import android.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;

/**
 * Predicate implementation
 *
 * @see Expression
 */
class PredicateImpl extends ExpressionImpl {

    private enum PredicateType {
        IS_NULL("IS NULL"),
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
    private Variable<?, ?> x;
    private Variable<?, ?> y;


    /**
     * Constructor
     *
     * @param   operation   operation
     * @param   db          database object
     * @param   x           first variable
     * @param   y           second variable
     */
    private PredicateImpl(PredicateType operation, DatabaseObject db, Variable<?, ?> x, Variable<?, ?> y) {
        super(null, null, null);

        this.operation = operation;
        this.db = db;
        this.x = x;
        this.y = y;
    }


    /**
     * Create "is null" predicate
     *
     * @param   db  database object
     * @param   x   variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if the variable is null
     */
    public static PredicateImpl isNull(DatabaseObject db, Variable<?, ?> x) {
        if (x == null)
            throw new QueryException("Variable can't be null");

        return new PredicateImpl(PredicateType.IS_NULL, db, x, null);
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
    public static PredicateImpl eq(DatabaseObject db, Variable<?, ?> x, Variable<?, ?> y) {
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
    public static PredicateImpl gt(DatabaseObject db, Variable<?, ?> x, Variable<?, ?> y) {
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
    public static PredicateImpl ge(DatabaseObject db, Variable<?, ?> x, Variable<?, ?> y) {
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
    public static PredicateImpl lt(DatabaseObject db, Variable<?, ?> x, Variable<?, ?> y) {
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
    public static PredicateImpl le(DatabaseObject db, Variable<?, ?> x, Variable<?, ?> y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.LE, db, x, y);
    }


    /**
     * Get string representation to be used in SQL query
     *
     * @return  string representation
     * @throws  QueryException  if the requested configuration is invalid
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Single operand predicate
        if (y == null) {
            if (operation == PredicateType.IS_NULL) {
                if (x.getData() instanceof Property) {
                    List<String> columns = convertProperty((Property) x.getData(), x.getTableAlias());

                    String separator = "";
                    for (String column : columns) {
                        sb.append(separator).append(column).append(" ").append(operation);
                        separator = " AND ";
                    }
                } else {
                    throw new QueryException("Invalid parameter");
                }
            }

            return sb.toString();
        }

        // Double operand predicate
        Object xData = x.getData();
        Object yData = y.getData();

        if (xData instanceof Property && yData instanceof Property) {
            // Two properties
            List<Pair<String, String>> pairs = bindProperties((Property)xData, x.getTableAlias(), (Property)yData, y.getTableAlias());

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


    /**
     * Get columns of a property
     *
     * @param   property    property
     * @param   alias       table alias
     *
     * @return  list of columns
     *
     * @throws  QueryException  if the property is invalid
     */
    private List<String> convertProperty(Property property, String alias) {
        List<String> result = new ArrayList<>();
        Field field;

        // Fully qualified alias
        String fullAlias = alias + property.getFieldParentClass().getSimpleName();

        // Get field
        try {
            field = property.getEntityClass().getField(property.getFieldName());
        } catch (NoSuchFieldException e) {
            throw new QueryException("Field " + property.getFieldName() + " not found in entity " + property.getEntityClass().getSimpleName());
        }

        // @Column
        if (field.isAnnotationPresent(Column.class)) {
            Column annotation = field.getAnnotation(Column.class);
            result.add(fullAlias + "." + annotation.name());
            return result;
        }

        // @JoinColumn
        if (field.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn annotation = field.getAnnotation(JoinColumn.class);
            result.add(fullAlias + "." + annotation.name());
            return result;
        }

        // @JoinColumns
        if (field.isAnnotationPresent(JoinColumns.class)) {
            JoinColumns annotation = field.getAnnotation(JoinColumns.class);

            for (JoinColumn joinColumn : annotation.value()) {
                result.add(fullAlias + "." + joinColumn.name());
            }

            return result;
        }

        throw new QueryException("Invalid parameter");
    }


    /**
     * Create property-property associations for the query
     *
     * @param   xProperty   first property
     * @param   xAlias      first table alias
     * @param   yProperty   second property
     * @param   yAlias      second table alias
     *
     * @return  list of columns pairs
     *
     * @throws  QueryException  if the requested configuration is invalid
     */
    private List<Pair<String, String>> bindProperties(Property xProperty, String xAlias, Property yProperty, String yAlias) {
        List<Pair<String, String>> result = new ArrayList<>();
        Field xField, yField;


        // Fully qualified aliases
        String xFullAlias = xAlias + xProperty.getFieldParentClass().getSimpleName();
        String yFullAlias = yAlias + yProperty.getFieldParentClass().getSimpleName();


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

        if (!xField.getType().isAssignableFrom(yField.getType()) && !yField.getType().isAssignableFrom(xField.getType()))
            throw new QueryException("Incompatible types: " + xField.getType().getSimpleName() + ", " + yField.getType().getSimpleName());


        // @Column
        if (xField.isAnnotationPresent(Column.class) && yField.isAnnotationPresent(Column.class)) {
            Column xAnnotation = xField.getAnnotation(Column.class);
            Column yAnnotation = yField.getAnnotation(Column.class);

            String xColumn = xFullAlias + "." + xAnnotation.name();
            String yColumn = yFullAlias + "." + yAnnotation.name();

            result.add(new Pair<>(xColumn, yColumn));
            return result;
        }


        // @JoinColumn
        if (xField.isAnnotationPresent(JoinColumn.class) && yField.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn xAnnotation = xField.getAnnotation(JoinColumn.class);
            JoinColumn yAnnotation = yField.getAnnotation(JoinColumn.class);

            String xColumn = xFullAlias + "." + xAnnotation.name();
            String yColumn = yFullAlias + "." + yAnnotation.name();

            result.add(new Pair<>(xColumn, yColumn));
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
                        String xColumn = xFullAlias + "." + xJoinColumn.name();
                        String yColumn = yFullAlias + "." + yJoinColumn.name();

                        result.add(new Pair<>(xColumn, yColumn));
                        continue outer;
                    }
                }
            }

            return result;
        }


        throw new QueryException("Invalid parameters");
    }


    /**
     * Create property-value associations for the query
     *
     * @param   db          database object
     * @param   property    property
     * @param   alias       table alias
     * @param   obj         object value
     *
     * @return  list of column-value pairs
     *
     * @throws  QueryException  if the requested configuration is invalid
     */
    private List<Pair<String, String>> bindPropertyObject(DatabaseObject db, Property property, String alias, Object obj) {
        List<Pair<String, String>> result = new ArrayList<>();
        Field field;


        // Fully qualified alias
        String fullAlias = alias + property.getFieldParentClass().getSimpleName();


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
            String column = fullAlias + "." + annotation.name();
            result.add(new Pair<>(column, objectToString(obj)));
            return result;
        }


        // @JoinColumn
        if (field.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn annotation = field.getAnnotation(JoinColumn.class);
            String column = fullAlias + "." + annotation.name();
            EntityObject referencedEntity = db.entities.get(field.getType());
            ColumnObject referecedColumn = referencedEntity.columnsNameMap.get(annotation.referencedColumnName());

            try {
                assert referecedColumn.field != null;
                Object value = referecedColumn.field.get(obj);
                result.add(new Pair<>(column, objectToString(value)));
            } catch (IllegalAccessException e) {
                throw new QueryException("Can't access field " + field.getName() + " of class " + field.getType().getSimpleName());
            }
        }


        // @JoinColumns
        if (field.isAnnotationPresent(JoinColumns.class)) {
            JoinColumns annotation = field.getAnnotation(JoinColumns.class);

            for (JoinColumn joinColumn : annotation.value()) {
                String column = fullAlias + "." + joinColumn.name();
                EntityObject referencedEntity = db.entities.get(field.getType());
                ColumnObject referencedColumn = referencedEntity.columnsNameMap.get(joinColumn.referencedColumnName());

                try {
                    assert referencedColumn.field != null;
                    Object value = referencedColumn.field.get(obj);
                    result.add(new Pair<>(column, objectToString(value)));
                } catch (IllegalAccessException e) {
                    throw new QueryException("Can't access field " + field.getName() + " of class " + field.getType().getSimpleName());
                }
            }

            return result;
        }


        throw new QueryException("Invalid parameters");
    }


    /**
     * Convert object to  string in order to be placed in the query
     *
     * @param   obj     object
     * @return  string
     */
    private static String objectToString(Object obj) {
        if (obj instanceof String) {
            return "\"" + obj + "\"";
        } else {
            return String.valueOf(obj);
        }
    }

}