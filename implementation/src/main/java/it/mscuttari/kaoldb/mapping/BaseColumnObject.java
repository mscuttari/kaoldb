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

package it.mscuttari.kaoldb.mapping;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import it.mscuttari.kaoldb.ConcurrentSession;
import it.mscuttari.kaoldb.exceptions.MappingException;
import it.mscuttari.kaoldb.exceptions.PojoException;
import it.mscuttari.kaoldb.interfaces.EntityManager;

import static it.mscuttari.kaoldb.StringUtils.escape;

/**
 * Representation of the basic properties of a column.
 *
 * @see SimpleColumnObject
 * @see JoinColumnObject
 * @see DiscriminatorColumnObject
 */
public abstract class BaseColumnObject implements ColumnsContainer {

    /** Database */
    @NonNull
    protected final DatabaseObject db;

    /** Entity the column belongs to */
    @NonNull
    protected final EntityObject<?> entity;

    /** Mapping session */
    private final ConcurrentSession<?> mappingSession = new ConcurrentSession<>();

    /** Column name */
    public String name;

    /** Custom column definition */
    @Nullable
    public String customColumnDefinition;

    /** Column type */
    public Class<?> type;

    /** Nullable column property */
    public Boolean nullable;

    /** Primary key column property */
    public Boolean primaryKey;

    /** Unique column property */
    public Boolean unique;

    /** Default value */
    @Nullable
    public String defaultValue;

    /**
     * Constructor.
     *
     * @param db                database
     * @param entity            entity the column belongs to
     */
    public BaseColumnObject(@NonNull DatabaseObject db,
                            @NonNull EntityObject<?> entity) {

        this.db = db;
        this.entity = entity;
    }

    @NonNull
    @Override
    public final String toString() {
        return name;
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof BaseColumnObject))
            return false;

        BaseColumnObject columnObject = (BaseColumnObject) obj;
        return name.equals(columnObject.name);
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }

    @NonNull
    @Override
    public final Iterator<BaseColumnObject> iterator() {
        return new SingleColumnIterator(this);
    }

    @Override
    public final void map() {
        mappingSession.submit(this::mapAsync);
    }

    /**
     * Actions that are executed asynchronously in order to load the column properties.
     */
    protected abstract void mapAsync();

    @Override
    public final void waitUntilMapped() {
        try {
            mappingSession.waitForAll();

        } catch (ExecutionException | InterruptedException e) {
            throw new MappingException(e);
        }
    }

    /**
     * Extract from an object the value associated to this column.
     *
     * @param obj       object to get the value from
     * @return value
     */
    @Nullable
    public abstract Object getValue(Object obj);

    /**
     * Set the object's field associated to this column to a given value.
     *
     * @param obj       object containing the field to be set
     * @param value     value to be set
     */
    public abstract void setValue(Object obj, Object value);

    /**
     * Check if the column leads to a relationship with another entity.
     *
     * @return <code>true</code> if the column has a relationship with another one;
     *         <code>false</code> otherwise
     */
    public abstract boolean hasRelationship();

    /**
     * Check if the object returned by {@link #getValue(Object)} already exists in the database.
     *
     * @param obj               object containing this column
     * @param entityManager     entity manager
     *
     * @return <code>true</code> if the data already exits in the database; <code>false</code> otherwise
     */
    public abstract boolean isDataExisting(Object obj, EntityManager entityManager);

    /**
     * Extract the column value from a {@link Cursor}.
     *
     * @param c         cursor
     * @param alias     alias of the table
     *
     * @return column value
     *
     * @throws PojoException if the data type is not compatible with the one found in the cursor
     */
    @SuppressWarnings("unchecked")
    public final Object parseCursor(Cursor c, String alias) {
        int columnIndex = c.getColumnIndexOrThrow(alias + "." + name);
        int columnType = c.getType(columnIndex);

        Object value = null;

        if (columnType == Cursor.FIELD_TYPE_INTEGER) {
            if (type.equals(Integer.class) || type.equals(int.class)) {
                value = c.getInt(columnIndex);

            } else if (type.equals(Long.class) || type.equals(long.class)) {
                value = c.getLong(columnIndex);

            } else if (type.equals(Date.class)) {
                value = new Date(c.getLong(columnIndex));

            } else if (type.equals(Calendar.class)) {
                value = Calendar.getInstance();
                ((Calendar) value).setTimeInMillis(c.getLong(columnIndex));

            } else {
                throw new PojoException("Incompatible data type: expected " + type.getSimpleName() + ", found Integer");
            }

        } else if (columnType == Cursor.FIELD_TYPE_FLOAT) {
            if (type.equals(Float.class) || type.equals(float.class)) {
                value = c.getFloat(columnIndex);

            } else if (type.equals(Double.class) || type.equals(double.class)) {
                value = c.getDouble(columnIndex);

            } else {
                throw new PojoException("Incompatible data type: expected " + type.getSimpleName() + ", found Float");
            }

        } else if (columnType == Cursor.FIELD_TYPE_STRING) {
            if (type.isEnum()) {
                value = Enum.valueOf((Class<Enum>) type, c.getString(columnIndex));

            } else if (String.class.isAssignableFrom(type)) {
                value = c.getString(columnIndex);

            } else {
                throw new PojoException("Incompatible data type: expected " + type.getSimpleName() + ", found String");
            }
        }

        return value;
    }

    /**
     * Insert a value into {@link ContentValues}.
     *
     * <p>
     * The conversion of the <code>value</code> follows these rules:
     * <ul>
     *      <li>If the column name is already in use, its value is replaced with the newer one</li>
     *      <li>Passing a <code>null</code> value will result in a <code>NULL</code> table column</li>
     *      <li>An empty string (<code>""</code>) will not generate a <code>NULL</code> column but
     *      a string with a length of zero (empty string)</li>
     *      <li>Boolean values are stored either as <code>1</code> (<code>true</code>) or
     *      <code>0</code> (<code>false</code>)</li>
     *      <li>{@link Date} and {@link Calendar} objects are stored by saving their time in
     *      milliseconds, which is got by respectively calling {@link Date#getTime()} and
     *      {@link Calendar#getTimeInMillis()}</li>
     * </ul>
     * </p>
     *
     * @param cv        content values
     * @param column    column name
     * @param value     value
     */
    public static void insertIntoContentValues(ContentValues cv, String column, Object value) {
        if (value == null) {
            cv.putNull(column);

        } else {
            if (value instanceof Enum) {
                cv.put(column, ((Enum<?>) value).name());

            } else if (value instanceof Integer) {
                cv.put(column, (int) value);

            } else if (value instanceof Long) {
                cv.put(column, (long) value);

            } else if (value instanceof Float) {
                cv.put(column, (float) value);

            } else if (value instanceof Double) {
                cv.put(column, (double) value);

            } else if (value instanceof String) {
                cv.put(column, (String) value);

            } else if (value instanceof Boolean) {
                cv.put(column, ((boolean) value) ? 1 : 0);

            } else if (value instanceof Date) {
                cv.put(column, ((Date) value).getTime());

            } else if (value instanceof Calendar) {
                cv.put(column, ((Calendar) value).getTimeInMillis());
            }
        }
    }

    /**
     * Get the column SQL statement to be used in the create table query.
     *
     * <p>
     * The columns definition takes into consideration the following parameters:
     * <ul>
     *      <li><b>Name</b></li>
     *      <li><b>Column definition</b>: if specified, the following parameters are skipped</li>
     *      <li><b>Type</b>: the column type determination is based on the field type and with respect of
     *            the following associations:
     *            <ul>
     *                  <li>int, Integer   => INTEGER</li>
     *                  <li>long, Long     => INTEGER</li>
     *                  <li>float, Float   => REAL</li>
     *                  <li>double, Double => REAL</li>
     *                  <li>String         => TEXT</li>
     *                  <li>Date, Calendar => INTEGER (date is stored as milliseconds from epoch)</li>
     *                  <li>Enum           => TEXT (enum constant name)</li>
     *                  <li>Anything else  => BLOB</li>
     *           </ul>
     *      </li>
     *      <li><b>Nullability</b></li>
     *      <li><b>Uniqueness</b></li>
     *      <li><b>Default value</b></li>
     * </ul>
     * Example: <code>"column 1" REAL NOT NULL</code>
     * </p>
     *
     * @return SQL statement
     */
    public final String getSQL() {
        StringBuilder result = new StringBuilder();

        // Column name
        result.append(escape(name));

        // Custom column definition
        if (customColumnDefinition != null && !customColumnDefinition.isEmpty()) {
            result.append(" ").append(customColumnDefinition);
            return result.toString();
        }

        // Column type
        result.append(" ").append(classToDbType(type));

        // Nullable
        if (!nullable) {
            result.append(" NOT NULL");
        }

        // Unique
        if (unique) {
            result.append(" UNIQUE");
        }

        // Default value
        if (defaultValue != null && !defaultValue.isEmpty()) {
            result.append(" DEFAULT '").append(escape(defaultValue)).append("'");
        }

        return result.toString();
    }

    /**
     * Get the database column type corresponding to a given Java class
     *
     * @param clazz     Java class
     * @return column type
     */
    public static String classToDbType(Class<?> clazz) {
        if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return "INTEGER";
        } else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            return "INTEGER";
        } else if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            return "REAL";
        } else if (clazz.equals(double.class) || clazz.equals(Double.class)) {
            return "REAL";
        } else if (clazz.equals(String.class)) {
            return "TEXT";
        } else if (clazz.equals(Date.class) || clazz.equals(Calendar.class)) {
            return "INTEGER";
        } else if (clazz.isEnum()) {
            return "TEXT";
        } else {
            return "BLOB";
        }
    }

    /**
     * Fake iterator to be used to iterate on a single column.
     */
    private static class SingleColumnIterator implements Iterator<BaseColumnObject> {

        private BaseColumnObject column;

        /**
         * Constructor.
         *
         * @param column    column to be returned during iteration
         */
        public SingleColumnIterator(BaseColumnObject column) {
            this.column = column;
        }

        @Override
        public boolean hasNext() {
            return column != null;
        }

        @Override
        public BaseColumnObject next() {
            try {
                return column;
            } finally {
                column = null;
            }
        }

    }

}
