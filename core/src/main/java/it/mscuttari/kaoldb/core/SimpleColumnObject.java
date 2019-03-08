package it.mscuttari.kaoldb.core;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;

import static it.mscuttari.kaoldb.core.ConcurrencyUtils.doAndNotifyAll;

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

        super(db, entity, field, 6);
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

        ExecutorService executorService = KaolDB.getInstance().getExecutorService();

        executorService.submit(() -> {
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
     * // TODO: check for data type compatibility
     */
    private void loadDefaultValue() {
        Column annotation = field.getAnnotation(Column.class);
        String result = annotation.defaultValue().isEmpty() ? null : annotation.defaultValue();
        doAndNotifyAll(this, () -> defaultValue = result);
    }


    @Override
    public void addToContentValues(@NonNull ContentValues cv, Object obj) {
        Object value = getValue(obj);
        PojoAdapter.insertDataIntoContentValues(cv, name, value);
    }

}
