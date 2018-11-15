package it.mscuttari.kaoldb.core;

import android.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;

/**
 * Predicate implementation
 *
 * @see Expression
 */
class PredicateImpl extends ExpressionImpl {

    private enum PredicateType {
        IS_NULL ("IS NULL", PredicateCardinality.UNARY),
        EQUAL   ("=",       PredicateCardinality.BINARY),
        GT      (">",       PredicateCardinality.BINARY),
        GE      (">=",      PredicateCardinality.BINARY),
        LT      ("<",       PredicateCardinality.BINARY),
        LE      ("<=",      PredicateCardinality.BINARY);

        private String operation;
        private PredicateCardinality cardinality;

        PredicateType(String operation, PredicateCardinality cardinality) {
            this.operation = operation;
            this.cardinality = cardinality;
        }

        @Override
        public String toString() {
            return operation;
        }

        public PredicateCardinality getCardinality() {
            return cardinality;
        }
    }


    private enum PredicateCardinality {
        UNARY,
        BINARY
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
        if (operation.getCardinality() == PredicateCardinality.UNARY) {
            return processUnaryPredicate();
        } else {
            return processBinaryPredicate();
        }
    }


    /**
     * Get first variable
     *
     * @return  first variable
     */
    public Variable<?, ?> getFirstVariable() {
        return x;
    }


    /**
     * Get second variable
     *
     * @return  second variable
     */
    public Variable<?, ?> getSecondVariable() {
        return y;
    }


    /**
     * Get string representation of an unary predicate
     *
     * @return  string representation to be used in query
     */
    private String processUnaryPredicate() {
        StringBuilder sb = new StringBuilder();

        Object data = x.getData();

        if (operation == PredicateType.IS_NULL) {
            if (data instanceof Property) {
                List<String> columns = getPropertyColumns((Property)data, x.getTableAlias());

                String separator = "";
                for (String column : columns) {
                    sb.append(separator).append(column).append(" ").append(operation);
                    separator = " AND ";
                }
            } else {
                sb.append(objectToString(data)).append(" ").append(operation);
            }
        }

        return sb.toString();
    }


    /**
     * Get string representation of an binary predicate
     *
     * @return  string representation to be used in query
     */
    private String processBinaryPredicate() {
        StringBuilder sb = new StringBuilder();

        Object xData = x.getData();
        Object yData = y.getData();

        if (xData instanceof Property && yData instanceof Property) {
            // Two properties
            List<Pair<String, String>> pairs = bindProperties(db, (Property) xData, x.getTableAlias(), (Property) yData, y.getTableAlias());

            String separator = "";
            for (Pair<String, String> pair : pairs) {
                sb.append(separator).append(pair.first).append(operation).append(pair.second);
                separator = " AND ";
            }


        } else if (xData instanceof Property) {
            // Property + value
            List<Pair<String, String>> pairs = bindPropertyObject(db, (Property) xData, x.getTableAlias(), yData);

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
     * Get the columns linked to a property
     *
     * In case of {@link Column} or {@link JoinColumn} annotated field:
     *  -   The result list will contain just one column.
     *
     * In case of {@link JoinColumns} annotated field:
     *  -   The result list will contain all the join columns.
     *
     * In case of {@link JoinTable} annotated field:
     *  -   The result list will contain the direct join columns. The inverse join columns are
     *      are not included because they are linked to the other join side table.
     *
     * Every column is in the format "tableAlias.columnName".
     *
     * @param   property    property
     * @param   alias       table alias
     *
     * @return  list of columns
     *
     * @throws  QueryException if the property is invalid
     */
    private static List<String> getPropertyColumns(Property<?, ?> property, String alias) {
        List<String> result = new ArrayList<>();

        // Fully qualified alias
        String fullAlias = From.getFullAlias(alias, property.fieldParentClass);

        // Get field
        Field field = property.getField();

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

        // @JoinTable
        if (field.isAnnotationPresent(JoinTable.class)) {
            JoinTable annotation = field.getAnnotation(JoinTable.class);

            for (JoinColumn joinColumn : annotation.joinColumns()) {
                result.add(fullAlias + "." + joinColumn.referencedColumnName());
            }

            return result;
        }

        throw new QueryException("Invalid parameter");
    }


    /**
     * Create property-property associations for the query
     *
     * @param   db          database object
     * @param   xProperty   first property
     * @param   xAlias      first table alias
     * @param   yProperty   second property
     * @param   yAlias      second table alias
     *
     * @return  list of columns pairs
     *
     * @throws  QueryException  if the requested configuration is invalid
     */
    private static List<Pair<String, String>> bindProperties(DatabaseObject db, Property xProperty, String xAlias, Property yProperty, String yAlias) {
        List<Pair<String, String>> result = new ArrayList<>();

        // Fully qualified aliases
        String xFullAlias = From.getFullAlias(xAlias, xProperty.fieldParentClass);
        String yFullAlias = From.getFullAlias(yAlias, yProperty.fieldParentClass);

        // Get fields
        Field xField = xProperty.getField();
        Field yField = yProperty.getField();

        if (!(xField.getType().isAssignableFrom(yField.getType()) && yField.getType().isAssignableFrom(xField.getType())))
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

        // @JoinTable
        if (xField.isAnnotationPresent(JoinTable.class) && yField.isAnnotationPresent(JoinTable.class)) {
            JoinTable xAnnotation = xField.getAnnotation(JoinTable.class);
            JoinTable yAnnotation = yField.getAnnotation(JoinTable.class);

            outer:
            for (JoinColumn xJoinColumn : xAnnotation.joinColumns()) {
                for (JoinColumn yJoinColumn : yAnnotation.joinColumns()) {
                    if (xJoinColumn.name().equals(yJoinColumn.name())) {
                        String xColumn = xFullAlias + "." + xJoinColumn.referencedColumnName();
                        String yColumn = yFullAlias + "." + yJoinColumn.referencedColumnName();

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
     * @param   property    model property
     * @param   alias       table alias
     * @param   obj         object value
     *
     * @return  list of column-value pairs
     *
     * @throws  QueryException  if the requested configuration is invalid
     */
    private static List<Pair<String, String>> bindPropertyObject(DatabaseObject db, Property<?, ?> property, String alias, Object obj) {
        List<Pair<String, String>> result = new ArrayList<>();

        // Get field
        Field field = property.getField();

        // Object type must be compatible with the property
        if (!field.getType().isAssignableFrom(obj.getClass()))
            throw new QueryException("Invalid object class");

        // @Column
        if (property.columnAnnotation == Column.class) {
            Column annotation = field.getAnnotation(Column.class);

            String fullAlias = From.getFullAlias(alias, property.fieldParentClass);
            String column = fullAlias + "." + annotation.name();

            result.add(new Pair<>(column, objectToString(obj)));

            return result;
        }

        // @JoinColumn, @JoinColumns, @JoinTable
        if (property.columnAnnotation == JoinColumn.class ||
                property.columnAnnotation == JoinColumns.class ||
                property.columnAnnotation == JoinTable.class) {

            EntityObject referencedEntity = db.getEntity(property.fieldType);

            String fullAlias = Join.getJoinFullAlias(alias, property.fieldParentClass, property.fieldType);

            for (ColumnObject primaryKey : referencedEntity.primaryKeys) {
                String column = fullAlias + "." + primaryKey.name;
                String primaryKeyValue = objectToString(primaryKey.getValue(obj));

                result.add(new Pair<>(column, primaryKeyValue));
            }

            return result;
        }

        throw new QueryException("Invalid parameters");
    }


    /**
     * Convert object to string in order to be placed in the query
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