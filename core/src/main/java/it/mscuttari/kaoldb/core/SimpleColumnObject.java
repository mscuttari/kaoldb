package it.mscuttari.kaoldb.core;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;

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
    public SimpleColumnObject(@NonNull DatabaseObject db,
                              @NonNull EntityObject entity,
                              @NonNull Field field) {

        super(db, entity, field,
                getColumnName(field),
                getCustomColumnDefinition(field),
                getType(field),
                getNullableProperty(field),
                getPrimaryKeyProperty(field),
                getUniqueProperty(field),
                getDefaultValue(field)
        );
    }


    /**
     * Get column name
     *
     * @param field     field the column is generated from
     * @return column name
     */
    @NonNull
    private static String getColumnName(@NonNull Field field) {
        Column annotation = field.getAnnotation(Column.class);
        return annotation.name().isEmpty() ? getDefaultName(field) : annotation.name();
    }


    /**
     * Get custom column definition
     *
     * @param field     field the column is generated from
     * @return custom column definition (null if not provided)
     */
    @Nullable
    private static String getCustomColumnDefinition(@NonNull Field field) {
        Column annotation = field.getAnnotation(Column.class);
        return annotation.columnDefinition().isEmpty() ? null : annotation.columnDefinition();
    }


    /**
     * Get column type
     *
     * @param field     field the column is generated from
     * @return column type
     */
    @NonNull
    private static Class<?> getType(@NonNull Field field) {
        return field.getType();
    }


    /**
     * Get nullable property
     *
     * @param field     field the column is generated from
     * @return whether the column is nullable or not
     */
    private static boolean getNullableProperty(@NonNull Field field) {
        Column annotation = field.getAnnotation(Column.class);
        return annotation.nullable();
    }


    /**
     * Get primary key property
     *
     * @param field     field the column is generated from
     * @return whether the column is a primary key or not
     */
    private static boolean getPrimaryKeyProperty(@NonNull Field field) {
        return field.isAnnotationPresent(Id.class);
    }


    /**
     * Get unique property
     *
     * @param field     field the column is generated from
     * @return whether the column value should be unique or not
     */
    private static boolean getUniqueProperty(@NonNull Field field) {
        Column annotation = field.getAnnotation(Column.class);
        return annotation.unique();
    }


    /**
     * Get default value
     *
     * The value is stored as a string in order to be ready for SQL statement generation.
     * // TODO: check for data type compatibility
     *
     * @param field     field the column is generated from
     * @return column default value (null if not provided)
     */
    @Nullable
    private static String getDefaultValue(@NonNull Field field) {
        Column annotation = field.getAnnotation(Column.class);
        return annotation.defaultValue().isEmpty() ? null : annotation.defaultValue();
    }


    @Override
    public void fixType(Map<Class<?>, EntityObject> entities) {
        // Nothing to be done. The column represent a basic entity attribute and therefore its
        // type is directly the field one (e.g. String or Integer).
    }


    @Override
    public void addToContentValues(@NonNull ContentValues cv, Object obj) {
        Object value = getValue(obj);
        PojoAdapter.insertDataIntoContentValues(cv, name, value);
    }

}
