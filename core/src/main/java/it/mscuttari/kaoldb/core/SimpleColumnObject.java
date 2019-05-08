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

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.exceptions.MappingException;

import static it.mscuttari.kaoldb.core.ConcurrentSession.doAndNotifyAll;

/**
 * This class allows to map a column acting as a basic entity attribute
 *
 * @see Column
 */
final class SimpleColumnObject extends BaseColumnObject {

    /**
     * Constructor
     *
     * @param db        database
     * @param entity    entity the column belongs to
     * @param field     field the column is generated from
     */
    private SimpleColumnObject(@NonNull DatabaseObject db,
                               @NonNull EntityObject<?> entity,
                               @NonNull Field field) {

        super(db, entity, field);
    }


    /**
     * Create the SimpleColumnObject linked to a field annotated with {@link Column}
     * and start the mapping process.
     *
     * @param db        database
     * @param entity    entity the column belongs to
     * @param field     field the column is generated from
     *
     * @return column object
     */
    public static SimpleColumnObject map(@NonNull DatabaseObject db,
                                         @NonNull EntityObject<?> entity,
                                         @NonNull Field field) {

        SimpleColumnObject result = new SimpleColumnObject(db, entity, field);
        result.loadName();

        ConcurrentSession concurrentSession = new ConcurrentSession();

        concurrentSession.submit(() -> {
            result.loadCustomColumnDefinition();
            result.loadType();
            result.loadNullableProperty();
            result.loadPrimaryKeyProperty();
            result.loadUniqueProperty();
            result.loadDefaultValue();

            doAndNotifyAll(entity.columns, () -> entity.columns.mappingStatus.decrementAndGet());
        });

        return result;
    }


    /**
     * Determine the column name
     */
    private void loadName() {
        Column annotation = field.getAnnotation(Column.class);
        String result = annotation.name().isEmpty() ? getDefaultName(field) : annotation.name();
        doAndNotifyAll(this, () -> name = result);
    }


    /**
     * Determine the custom column definition
     */
    private void loadCustomColumnDefinition() {
        Column annotation = field.getAnnotation(Column.class);
        String result = annotation.columnDefinition().isEmpty() ? null : annotation.columnDefinition();
        doAndNotifyAll(this, () -> customColumnDefinition = result);
    }


    /**
     * Determine the column type
     */
    private void loadType() {
        Class<?> result = field.getType();
        doAndNotifyAll(this, () -> type = result);
    }


    /**
     * Determine whether the column is nullable or not
     */
    private void loadNullableProperty() {
        Column annotation = field.getAnnotation(Column.class);
        boolean result = annotation.nullable();
        doAndNotifyAll(this, () -> nullable = result);
    }


    /**
     * Determine whether the column is a primary key or not
     */
    private void loadPrimaryKeyProperty() {
        boolean result = field.isAnnotationPresent(Id.class);
        doAndNotifyAll(this, () -> primaryKey = result);
    }


    /**
     * Determine whether the column value should be unique or not
     */
    private void loadUniqueProperty() {
        Column annotation = field.getAnnotation(Column.class);
        boolean result = annotation.unique();
        doAndNotifyAll(this, () -> unique = result);
    }


    /**
     * Determine the default value
     *
     * The value is stored as a string in order to be ready for SQL statement generation.
     */
    private void loadDefaultValue() {
        Column annotation = field.getAnnotation(Column.class);
        String def = annotation.defaultValue().isEmpty() ? null : annotation.defaultValue();

        // Check data compatibility
        if (def != null) {
            try {
                if (type.isAssignableFrom(Integer.class) || type.isAssignableFrom(int.class)) {
                    def = String.valueOf(Integer.parseInt(def));

                } else if (type.isAssignableFrom(Long.class) || type.isAssignableFrom(long.class)) {
                    def = String.valueOf(Long.parseLong(def));

                } else if (type.isAssignableFrom(Float.class) || type.isAssignableFrom(float.class)) {
                    def = String.valueOf(Float.parseFloat(def));

                } else if (type.isAssignableFrom(Double.class) || type.isAssignableFrom(double.class)) {
                    def = String.valueOf(Double.parseDouble(def));

                } else if (type.isAssignableFrom(Boolean.class) || type.isAssignableFrom(boolean.class)) {
                    boolean value = Boolean.parseBoolean(def);
                    def = value ? "1" : "0";

                } else if (type.isAssignableFrom(Date.class)) {
                    // Need to be a timestamp
                    def = String.valueOf(Long.parseLong(def));

                } else if (type.isAssignableFrom(Calendar.class)) {
                    // Need to be a timestamp
                    def = String.valueOf(Long.parseLong(def));
                }

            } catch (Exception e) {
                throw new MappingException("[Column \"" + name + "\"] default value is incompatible with type " + type.getSimpleName());
            }
        }

        String result = def;
        doAndNotifyAll(this, () -> defaultValue = result);
    }


    @Override
    public void addToContentValues(@NonNull ContentValues cv, Object obj) {
        Object value = getValue(obj);
        insertIntoContentValues(cv, name, value);
    }


    @Override
    public boolean hasRelationship() {
        return false;
    }

}
