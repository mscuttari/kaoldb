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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import it.mscuttari.kaoldb.exceptions.PojoException;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

/**
 * Column generated from a class field.
 */
public abstract class FieldColumnObject extends BaseColumnObject {

    /** Field the column is generated from */
    @NonNull
    public final Field field;


    /**
     * Constructor.
     *
     * @param db        database
     * @param entity    entity the column belongs to
     * @param field     field the column is generated from
     */
    public FieldColumnObject(@NonNull DatabaseObject db,
                            @NonNull EntityObject<?> entity,
                            @NonNull Field field) {

        super(db, entity);

        this.field = field;
    }


    @Nullable
    @Override
    public final Object getValue(Object obj) {
        if (obj == null) {
            return null;
        }

        field.setAccessible(true);

        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new PojoException(e);
        }
    }


    @Override
    public final void setValue(Object obj, Object value) {
        field.setAccessible(true);

        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new PojoException(e);
        }
    }


    @Override
    public boolean isDataExisting(Object obj, EntityManager entityManager) {
        Object fieldValue = getValue(obj);

        // Null data is considered to be existing
        if (fieldValue == null) {
            return true;
        }

        Class<?> clazz = fieldValue.getClass();

        // Primitive data can't be stored alone in the database because they have no table associated.
        // Therefore, primitive data existence check should be skipped (this way it is always
        // considered successful)

        if (isPrimitiveType(clazz)) {
            return true;
        }

        // Non-primitive data existence must be checked and so a select query is executed
        // The select query is based on the primary keys of the entity
        EntityObject<?> entity = db.getEntity(clazz);

        QueryBuilder<?> qb = entityManager.getQueryBuilder(entity.clazz);
        Root root = qb.getRoot(entity.clazz);

        Expression where = null;

        for (FieldColumnObject primaryKey : entity.columns.getPrimaryKeys()) {
            // Create the property that the generated class would have because of the primary key field
            SingleProperty property = new SingleProperty<>(entity.clazz, primaryKey.field.getType(), primaryKey.field);

            Object value = primaryKey.getValue(fieldValue);
            Expression expression = root.eq(property, value);

            // In case of multiple primary keys, concatenated the WHERE expressions
            where = where == null ? expression : where.and(expression);
        }

        // Run the query and check if its result is not an empty set
        Object queryResult = qb.from(root).where(where).build(root).getSingleResult();
        return queryResult != null;
    }


    /**
     * Check if the data class can be stored in the database as if it is a primitive type.
     *
     * @param clazz     data class
     * @return <code>true</code> if a "primitive-compatible class" is found;
     *         <code>false</code> otherwise
     */
    private static boolean isPrimitiveType(Class<?> clazz) {
        return  Enum.class.isAssignableFrom(clazz) ||
                boolean.class.isAssignableFrom(clazz) ||
                Boolean.class.isAssignableFrom(clazz) ||
                byte.class.isAssignableFrom(clazz) ||
                Byte.class.isAssignableFrom(clazz) ||
                char.class.isAssignableFrom(clazz) ||
                Character.class.isAssignableFrom(clazz) ||
                short.class.isAssignableFrom(clazz) ||
                Short.class.isAssignableFrom(clazz) ||
                int.class.isAssignableFrom(clazz) ||
                Integer.class.isAssignableFrom(clazz) ||
                long.class.isAssignableFrom(clazz) ||
                Long.class.isAssignableFrom(clazz) ||
                float.class.isAssignableFrom(clazz) ||
                Float.class.isAssignableFrom(clazz) ||
                double.class.isAssignableFrom(clazz) ||
                Double.class.isAssignableFrom(clazz) ||
                String.class.isAssignableFrom(clazz) ||
                Date.class.isAssignableFrom(clazz) ||
                Calendar.class.isAssignableFrom(clazz);
    }


    /**
     * Get the default name for a column.
     *
     * <p>Uppercase characters are replaced with underscore followed by the same character converted
     * to lowercase. Only the first character, if uppercase, is converted to lowercase avoiding
     * the underscore.<br>
     * Example: <code>columnFieldName</code> => <code>column_field_name</code></p>
     *
     * @param field     field the column is generated from
     * @return column name
     */
    protected static String getDefaultName(@NonNull Field field) {
        String fieldName = field.getName();
        char[] c = fieldName.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        fieldName = new String(c);
        return fieldName.replaceAll("([A-Z])", "_$1").toLowerCase();
    }

}
