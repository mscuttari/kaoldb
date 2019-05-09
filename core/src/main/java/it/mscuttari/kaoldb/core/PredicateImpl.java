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

import android.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.mscuttari.kaoldb.core.ExpressionImpl.ExpressionType.AND;

/**
 * Predicate implementation
 *
 * @see Expression
 */
final class PredicateImpl<T> implements ExpressionInt {

    private enum PredicateType {

        IS_NULL ("IS NULL", 1),
        EQUAL   ("=",       2),
        GT      (">",       2),
        GE      (">=",      2),
        LT      ("<",       2),
        LE      ("<=",      2);

        private final String operation;
        public final int cardinality;

        PredicateType(String operation, @IntRange(from = 1, to = 2) int cardinality) {
            this.operation   = operation;
            this.cardinality = cardinality;
        }

        @NonNull
        @Override
        public String toString() {
            return operation;
        }

    }


    @NonNull  private final PredicateType operation;
    @NonNull  private final DatabaseObject db;
    @NonNull  public final RootInt<?> root;
    @NonNull  public final Variable<T> x;
    @Nullable public final Variable<T> y;


    /**
     * Constructor.
     *
     * @param operation     operation
     * @param db            database
     * @param root          root the predicate is generated from
     * @param x             first variable
     * @param y             second variable
     */
    private PredicateImpl(@NonNull  PredicateType operation,
                          @NonNull  DatabaseObject db,
                          @NonNull  RootInt<?> root,
                          @NonNull  Variable<T> x,
                          @Nullable Variable<T> y) {

        this.operation = operation;
        this.db = db;
        this.root = root;
        this.x = checkNotNull(x);

        // If the cardinality is 1, y is allowed to be null because not used
        this.y = operation.cardinality == 1 ? y : checkNotNull(y);
    }


    /**
     * Create <code>"IS NULL"</code> predicate.
     *
     * @param db    database object
     * @param root  root the predicate is generated from
     * @param x     variable
     *
     * @return predicate
     */
    public static <T> PredicateImpl isNull(DatabaseObject db, RootInt<?> root, Variable<T> x) {
        return new PredicateImpl<>(PredicateType.IS_NULL, db, root, x, null);
    }


    /**
     * Create <code>"EQUALS"</code> predicate.
     *
     * @param db    database object
     * @param root  root the predicate is generated from
     * @param x     first variable
     * @param y     second variable
     *
     * @return predicate
     */
    public static <T> PredicateImpl eq(DatabaseObject db, RootInt<?> root, Variable<T> x, Variable<T> y) {
        return new PredicateImpl<>(PredicateType.EQUAL, db, root, x, y);
    }


    /**
     * Create <code>"GREATER THAN"</code> predicate.
     *
     * @param db    database object
     * @param root  root the predicate is generated from
     * @param x     first variable
     * @param y     second variable
     *
     * @return predicate
     */
    public static <T> PredicateImpl gt(DatabaseObject db, RootInt<?> root, Variable<T> x, Variable<T> y) {
        return new PredicateImpl<>(PredicateType.GT, db, root, x, y);
    }


    /**
     * Create <code>"GREATER OR EQUALS THAN"</code> predicate.
     *
     * @param db    database object
     * @param root  root the predicate is generated from
     * @param x     first variable
     * @param y     second variable
     *
     * @return predicate
     */
    public static <T> PredicateImpl ge(DatabaseObject db, RootInt<?> root, Variable<T> x, Variable<T> y) {
        return new PredicateImpl<>(PredicateType.GE, db, root, x, y);
    }


    /**
     * Create <code>"LESS THAN"</code> predicate.
     *
     * @param db    database object
     * @param root  root the predicate is generated from
     * @param x     first variable
     * @param y     second variable
     *
     * @return predicate
     */
    public static <T> PredicateImpl lt(DatabaseObject db, RootInt<?> root, Variable<T> x, Variable<T> y) {
        return new PredicateImpl<>(PredicateType.LT, db, root, x, y);
    }


    /**
     * Create <code>"LESS OR EQUAL THAN"</code> predicate.
     *
     * @param db    database object
     * @param root  root the predicate is generated from
     * @param x     first variable
     * @param y     second variable
     *
     * @return predicate
     */
    public static <T> PredicateImpl le(DatabaseObject db, RootInt<?> root, Variable<T> x, Variable<T> y) {
        return new PredicateImpl<>(PredicateType.LE, db, root, x, y);
    }


    /**
     * Get the string representation to be used in SQL query.
     *
     * <p>
     * Some examples:
     * <ul>
     *     <li>a1.column IS NULL</li>
     *     <li>a1.column = value</li>
     *     <li>a1.column = a2.column</li>
     *     <li>a1.column > value</li>
     * </ul>
     * </p>
     *
     * @return string representation of the predicate
     *
     * @throws QueryException if the requested configuration is invalid
     * @throws IllegalStateException if the operation cardinality is unexpected
     */
    @NonNull
    @Override
    public String toString() {
        if (operation.cardinality == 1) {
            return processUnaryPredicate();

        } else if (operation.cardinality == 2){
            return processBinaryPredicate();

        } else {
            throw new IllegalStateException("Unexpected cardinality: " + operation.cardinality);
        }
    }


    @NonNull
    @Override
    public Iterator<PredicateImpl> iterator() {
        return new SinglePredicateIterator(this);
    }


    @Override
    public Expression not() {
        return ExpressionImpl.not(this);
    }


    @Override
    public Expression and(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = ExpressionImpl.and(result, expression);
        }

        return result;
    }


    @Override
    public Expression or(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = ExpressionImpl.or(result, expression);
        }

        return result;
    }


    @Override
    public Expression xor(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = ExpressionImpl.xor(result, expression);
        }

        return result;
    }


    @Override
    public Expression nand(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = ExpressionImpl.nand(result, expression);
        }

        return result;
    }


    @Override
    public Expression nor(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = ExpressionImpl.nor(result, expression);
        }

        return result;
    }


    @Override
    public Expression xnor(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = ExpressionImpl.xnor(result, expression);
        }

        return result;
    }


    /**
     * Get string representation of an unary predicate.
     *
     * @return string representation to be used in query
     */
    private String processUnaryPredicate() {
        if (operation == PredicateType.IS_NULL) {
            if (x.hasProperty()) {
                return StringUtils.implode(
                        getPropertyColumns(x.getProperty(), x.getTableAlias()),
                        obj -> obj + " " + operation,
                        " " + AND + " "
                );

            } else {
                return objectToString(x.getRawData()) + " " + operation;
            }
        }

        // Normally not reachable
        throw new IllegalStateException("Unknown predicate operation: " + operation);
    }


    /**
     * Get string representation of an binary predicate.
     *
     * @return string representation to be used in query
     */
    private String processBinaryPredicate() {
        if (y == null) {
            // Security check. Normally not reachable.
            throw new IllegalStateException("Second variable is null");
        }

        if (x.hasProperty() && y.hasProperty()) {
            // Two properties
            return StringUtils.implode(
                    bindProperties(x.getProperty(), x.getTableAlias(), y.getProperty(), y.getTableAlias()),
                    obj -> obj.first + operation + obj.second,
                    " " + AND + " "
            );

        } else if (x.hasProperty()) {
            // Property + value
            return StringUtils.implode(
                    bindPropertyObject(x.getProperty(), y.getRawData()),
                    obj -> obj.first + operation + obj.second,
                    " " + AND + " "
            );

        } else {
            // Two values
            return objectToString(x.getRawData()) + operation + objectToString(y.getRawData());
        }
    }


    /**
     * Get the columns linked to a property.
     *
     * <p>
     *     In case of {@link Column} or {@link JoinColumn} annotated field:
     *     <ul>
     *         <li>The result list will contain just one column</li>
     *     </ul>
     *
     *     In case of {@link JoinColumns} annotated field:
     *     <ul>
     *         <li>The result list will contain all the join columns</li>
     *     </ul>
     *
     *     In case of {@link JoinTable} annotated field:
     *     <ul>
     *         <li>The result list will contain the direct join columns. The inverse join columns
     *         are not included because they are linked to the other join side table.</li>
     *     </ul>
     * </p>
     *
     * <p>Every column is in the format <code>"tableAlias.columnName"</code>.</p>
     *
     * @param property      property
     * @param alias         table alias
     *
     * @return list of columns
     *
     * @throws QueryException if the property is invalid
     */
    private static List<String> getPropertyColumns(Property<?, ?> property, String alias) {
        List<String> result = new ArrayList<>();

        // Get field
        Field field = property.getField();

        // @Column
        if (field.isAnnotationPresent(Column.class)) {
            Column annotation = field.getAnnotation(Column.class);
            result.add(alias + "." + annotation.name());
            return result;
        }

        // @JoinColumn
        if (field.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn annotation = field.getAnnotation(JoinColumn.class);
            result.add(alias + "." + annotation.name());
            return result;
        }

        // @JoinColumns
        if (field.isAnnotationPresent(JoinColumns.class)) {
            JoinColumns annotation = field.getAnnotation(JoinColumns.class);

            for (JoinColumn joinColumn : annotation.value()) {
                result.add(alias + "." + joinColumn.name());
            }

            return result;
        }

        // @JoinTable
        if (field.isAnnotationPresent(JoinTable.class)) {
            JoinTable annotation = field.getAnnotation(JoinTable.class);

            for (JoinColumn joinColumn : annotation.joinColumns()) {
                result.add(alias + "." + joinColumn.referencedColumnName());
            }

            return result;
        }

        throw new QueryException("Invalid parameter");
    }


    /**
     * Create property-property associations for the query.
     *
     * @param xProperty     first property
     * @param xAlias        first table alias
     * @param yProperty     second property
     * @param yAlias        second table alias
     *
     * @return list of columns pairs
     *
     * @throws QueryException if the requested configuration is invalid
     */
    private List<Pair<String, String>> bindProperties(Property xProperty, String xAlias, Property yProperty, String yAlias) {
        List<Pair<String, String>> result = new ArrayList<>();

        // Get fields
        Field xField = xProperty.getField();
        Field yField = yProperty.getField();

        if (!(xField.getType().isAssignableFrom(yField.getType()) && yField.getType().isAssignableFrom(xField.getType())))
            throw new QueryException("Incompatible types: " + xField.getType().getSimpleName() + ", " + yField.getType().getSimpleName());

        // @Column
        if (xField.isAnnotationPresent(Column.class) && yField.isAnnotationPresent(Column.class)) {
            Column xAnnotation = xField.getAnnotation(Column.class);
            Column yAnnotation = yField.getAnnotation(Column.class);

            String xColumn = xAlias + "." + xAnnotation.name();
            String yColumn = yAlias + "." + yAnnotation.name();

            result.add(new Pair<>(xColumn, yColumn));
            return result;
        }

        // @JoinColumn
        if (xField.isAnnotationPresent(JoinColumn.class) && yField.isAnnotationPresent(JoinColumn.class)) {
            JoinColumn xAnnotation = xField.getAnnotation(JoinColumn.class);
            JoinColumn yAnnotation = yField.getAnnotation(JoinColumn.class);

            String xColumn = xAlias + "." + xAnnotation.name();
            String yColumn = yAlias + "." + yAnnotation.name();

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
                        String xColumn = xAlias + "." + xJoinColumn.name();
                        String yColumn = yAlias + "." + yJoinColumn.name();

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
                        String xColumn = xAlias + "." + xJoinColumn.referencedColumnName();
                        String yColumn = yAlias + "." + yJoinColumn.referencedColumnName();

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
     * Create property-value associations for the query.
     *
     * @param obj           object value
     * @return list of column-value pairs
     * @throws QueryException if the requested configuration is invalid
     */
    private List<Pair<String, String>> bindPropertyObject(Property<?, T> property, T obj) {
        List<Pair<String, String>> result = new ArrayList<>();

        // Get field
        Field field = property.getField();

        // Object type must be compatible with the property
        if (!field.getType().isAssignableFrom(obj.getClass())) {
            throw new QueryException("Invalid object class");
        }

        // @Column
        if (property.columnAnnotation == Column.class) {
            Column annotation = field.getAnnotation(Column.class);
            String column = root.getAlias() + "." + annotation.name();
            result.add(new Pair<>(column, objectToString(obj)));

            return result;
        }

        // @JoinColumn, @JoinColumns, @JoinTable
        if (property.columnAnnotation == JoinColumn.class ||
                property.columnAnnotation == JoinColumns.class ||
                property.columnAnnotation == JoinTable.class) {

            EntityObject<T> referencedEntity = db.getEntity(property.fieldType);

            String referencedEntityAlias = isLeftVariableDerivedFromRoot() ?
                    root.getAlias() :
                    root.getAlias() + property.fieldName;

            for (BaseColumnObject primaryKey : referencedEntity.columns.getPrimaryKeys()) {
                String column = referencedEntityAlias + "." + primaryKey.name;
                String primaryKeyValue = objectToString(primaryKey.getValue(obj));

                result.add(new Pair<>(column, primaryKeyValue));
            }

            return result;
        }

        throw new QueryException("Invalid parameters");
    }


    /**
     * Convert object to string in order to be placed in the query.
     *
     * @param obj       object
     * @return string
     */
    private static String objectToString(Object obj) {
        if (obj instanceof String) {
            return "\"" + obj + "\"";
        } else {
            return String.valueOf(obj);
        }
    }


    /**
     * Check if {@link #x} derives from {@link #root}.
     *
     * @return true
     */
    public boolean isLeftVariableDerivedFromRoot() {
        boolean result = x.getTableAlias().equals(root.getAlias());

        if (x.hasProperty()) {
            Property<?, ?> property = x.getProperty();
            result &= root.getEntityClass().equals(property.fieldType);
        }

        return result;
    }


    /**
     * Fake iterator to be used to iterate on a single predicate.
     */
    private static class SinglePredicateIterator implements Iterator<PredicateImpl> {

        private PredicateImpl predicate;

        /**
         * Constructor.
         *
         * @param predicate     predicate to be returned during iteration
         */
        public SinglePredicateIterator(PredicateImpl predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean hasNext() {
            return predicate != null;
        }

        @Override
        public PredicateImpl next() {
            PredicateImpl result = predicate;
            predicate = null;
            return result;
        }

    }

}
