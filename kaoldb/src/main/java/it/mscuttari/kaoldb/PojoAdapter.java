package it.mscuttari.kaoldb;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.util.Log;

import it.mscuttari.kaoldb.exceptions.PojoException;
import it.mscuttari.kaoldb.exceptions.QueryException;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

class PojoAdapter {

    private PojoAdapter() {

    }

    /**
     * Convert cursor to POJO (plain old java object)
     *
     * @param   c               cursor
     * @param   entityClass     entity class of the POJO (just for return type)
     * @param   entity          entity representing the POJO
     *
     * @return  populated object
     *
     * @throws  PojoException   in case of error
     */
    @Nullable
    static <T> T cursorToObject(Cursor c, Class<T> entityClass, EntityObject entity) {
        if (entity.children.size() != 0) {
            // Go down to child class
            int columnIndex = c.getColumnIndex(entity.discriminatorColumn.name);
            Object discriminatorValue = null;

            if (entity.discriminatorColumn.type.equals(Integer.class)) {
                discriminatorValue = c.getInt(columnIndex);
            } else if (entity.discriminatorColumn.type.equals(String.class)) {
                discriminatorValue = c.getString(columnIndex);
            }

            for (EntityObject child : entity.children) {
                if (child.discriminatorValue.equals(discriminatorValue)) {
                    return cursorToObject(c, entityClass, child);
                }
            }

            throw new PojoException("Child class not found");

        } else {
            // Populate child class
            try {
                @SuppressWarnings("unchecked") T result = (T)entity.entityClass.newInstance();

                while (entity != null) {
                    if (entity.realTable) {
                        for (ColumnObject column : entity.columns) {
                            if (column.field == null) continue;

                            int columnIndex = c.getColumnIndex(column.name);
                            int columnType = c.getType(columnIndex);
                            Object value = null;

                            if (columnType == Cursor.FIELD_TYPE_INTEGER) {
                                value = c.getInt(columnIndex);
                            } else if (columnType == Cursor.FIELD_TYPE_FLOAT) {
                                value = c.getFloat(columnIndex);
                            } else if (columnType == Cursor.FIELD_TYPE_STRING) {
                                value = c.getString(columnIndex);
                            }

                            column.field.setAccessible(true);
                            column.field.set(result, value);
                        }
                    }

                    entity = entity.parent;
                }

                return result;

            } catch (InstantiationException e) {
                throw new PojoException(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new PojoException(e.getMessage());
            }
        }
    }


    private static void populateColumn() {

    }


    /**
     * Prepare the data to be saved in a particular entity
     *
     * @param   currentEntity       entity
     * @param   obj                 object
     *
     * @return  data ready to be saved in the database (null if the entity doesn't have a real table)
     */
    @Nullable
    static ContentValues objectToContentValues(EntityObject currentEntity, EntityObject childEntity, Object obj) {
        if (!currentEntity.realTable)
            return null;

        ContentValues cv = new ContentValues();

        // Normal columns
        for (ColumnObject column : currentEntity.columns) {
            if (column.field == null) continue;

            try {
                column.field.setAccessible(true);
                Object value = column.field.get(obj);
                insertDataIntoContentValues(cv, column.name, value);

            } catch (IllegalAccessException e) {
                throw new QueryException(e.getMessage());
            }
        }

        // Discriminator column
        if (childEntity != null) {
            Object value = childEntity.discriminatorValue;
            insertDataIntoContentValues(cv, currentEntity.discriminatorColumn.name, value);
        }

        return cv;
    }


    /**
     * Insert value into ContentValues
     *
     * @param   cv          content values
     * @param   columnName  column name
     * @param   value       value
     */
    private static void insertDataIntoContentValues(ContentValues cv, String columnName, Object value) {
        if (value == null) {
            cv.putNull(columnName);
        } else {
            if (value instanceof Integer || value.getClass().equals(int.class)) {
                cv.put(columnName, (int)value);
            } else if (value instanceof Long || value.getClass().equals(long.class)) {
                cv.put(columnName, (long)value);
            } else if (value instanceof Float || value.getClass().equals(float.class)) {
                cv.put(columnName, (float)value);
            } else if (value instanceof Double || value.getClass().equals(double.class)) {
                cv.put(columnName, (double)value);
            } else if (value instanceof String) {
                cv.put(columnName, (String)value);
            } else if (value instanceof Boolean || value.getClass().equals(boolean.class)) {
                cv.put(columnName, ((boolean)value) ? 1 : 0);
            }
        }
    }

}
