package it.mscuttari.kaoldb.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;

import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.exceptions.PojoException;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

class PojoAdapter {

    private PojoAdapter() {

    }


    /**
     * Convert cursor to POJO (plain old java object)
     *
     * @param   db              database object
     * @param   c               cursor
     * @param   cursorMap       map between cursor column names and column indexes
     * @param   entityClass     entity class of the POJO (just for return type)
     * @param   entity          entity representing the POJO
     *
     * @return  populated object
     *
     * @throws  PojoException   in case of error
     */
    @Nullable
    public static <T> T cursorToObject(DatabaseObject db, Cursor c, Map<String, Integer> cursorMap, Class<T> entityClass, EntityObject entity, String alias) {
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
                    return cursorToObject(db, c, cursorMap, entityClass, child, alias);
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

                            String columnName = alias + entity.getName() + "." + column.name;
                            int columnIndex = cursorMap.get(columnName);
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
                e.printStackTrace();
                throw new PojoException(e.getMessage());

            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new PojoException(e.getMessage());
            }
        }
    }


    /**
     * Prepare the data to be saved in a particular entity
     *
     * @param   context         context
     * @param   db              database object
     * @param   currentEntity   current entity object
     * @param   childEntity     child entity object
     * @param   obj             object to be persisted
     *
     * @return  data ready to be saved in the database (null if the entity doesn't have a real table)
     */
    static ContentValues objectToContentValues(Context context, DatabaseObject db, EntityObject currentEntity, EntityObject childEntity, Object obj) {
        ContentValues cv = new ContentValues();

        // Skip the entity if it doesn't have its own dedicated table
        if (!currentEntity.realTable)
            return cv;


        // Discriminator column
        if (childEntity != null) {
            if (cv.containsKey(currentEntity.discriminatorColumn.name)) {
                // Discriminator value has been manually set
                // Checking if it is in accordance with the child entity class
                Object specifiedDiscriminatorValue = cv.get(currentEntity.discriminatorColumn.name);

                if (specifiedDiscriminatorValue != null && !specifiedDiscriminatorValue.equals(childEntity.discriminatorValue))
                    throw new QueryException("Wrong discriminator value: expected " + childEntity.discriminatorValue + ", found " + specifiedDiscriminatorValue);

            } else {
                // The discriminator value has not been found. Adding it automatically
                Annotation discriminatorColumnAnnotation = currentEntity.discriminatorColumn.annotation;

                if (discriminatorColumnAnnotation instanceof Column) {
                    // The discriminator column is linked to a field annotated with @Column
                    insertDataIntoContentValues(cv, currentEntity.discriminatorColumn.name, childEntity.discriminatorValue);

                } else if (discriminatorColumnAnnotation instanceof JoinColumn) {
                    // The discriminator column is linked to a field annotated with @JoinColumn
                    Class<?> discriminatorType = currentEntity.discriminatorColumn.field.getType();
                    Object discriminator;

                    try {
                        // Create the discriminator object containing the discriminator column
                        discriminator = discriminatorType.newInstance();

                        // Set the child discriminator value
                        Field discriminatorField = discriminatorType.getField(((JoinColumn)discriminatorColumnAnnotation).referencedColumnName());
                        discriminatorField.set(discriminator, childEntity.discriminatorValue);

                        // Assign the discriminator value to the object to be persisted
                        currentEntity.discriminatorColumn.field.set(obj, discriminator);

                    } catch (InstantiationException e) {
                        throw new QueryException(e.getMessage());
                    } catch (IllegalAccessException e) {
                        throw new QueryException(e.getMessage());
                    } catch (NoSuchFieldException e) {
                        throw new QueryException(e.getMessage());
                    }

                    insertJoinColumnIntoContentValues(cv, obj, db, currentEntity.discriminatorColumn.field, (JoinColumn)discriminatorColumnAnnotation);

                } else if (discriminatorColumnAnnotation instanceof JoinColumns) {
                    // The discriminator column is linked to a field annotated with @JoinColumns
                    Class<?> discriminatorType = currentEntity.discriminatorColumn.field.getType();
                    Object discriminator;

                    try {
                        // Create the discriminator object containing the discriminator column
                        discriminator = discriminatorType.newInstance();

                        for (JoinColumn joinColumn : ((JoinColumns)discriminatorColumnAnnotation).value()) {
                            if (joinColumn.name().equals(currentEntity.discriminatorColumn.name)) {
                                // Set the child discriminator value
                                Field discriminatorField = discriminatorType.getField(joinColumn.referencedColumnName());
                                discriminatorField.set(discriminator, childEntity.discriminatorValue);

                                // Assign the discriminator value to the object to be persisted
                                currentEntity.discriminatorColumn.field.set(obj, discriminator);

                                break;
                            }
                        }

                    } catch (InstantiationException e) {
                        throw new QueryException(e.getMessage());
                    } catch (IllegalAccessException e) {
                        throw new QueryException(e.getMessage());
                    } catch (NoSuchFieldException e) {
                        throw new QueryException(e.getMessage());
                    }

                    insertJoinColumnsIntoContentValues(cv, obj, db, currentEntity.discriminatorColumn.field, (JoinColumns)discriminatorColumnAnnotation);

                } else if (discriminatorColumnAnnotation instanceof JoinTable) {
                    // The discriminator column is linked to a field annotated with @JoinTable
                    throw new QueryException("Not implemented");
                }
            }
        }


        // Normal columns
        List<Field> fields = new ArrayList<>();

        for (ColumnObject column : currentEntity.columns) {
            if (column.field != null && !fields.contains(column.field)) {
                fields.add(column.field);
                if (!checkDataExistence(obj, context, db, column.field))
                    throw new QueryException("Object " + column.field + " doesn't exist in the database. Persist it first!");

                insertFieldIntoContentValues(cv, obj, db, column.field);
            }
        }

        return cv;
    }


    /**
     * Check if the element already exists in the database
     *
     * @param   obj         object to be persisted
     * @param   context     context
     * @param   db          database object
     * @param   field       object field to be checked
     *
     * @return  boolean     true if the data exits; false otherwise
     */
    private static boolean checkDataExistence(Object obj, Context context, DatabaseObject db, Field field) {
        // Primitive data
        Class<?> fieldClass = field.getType();

        if (fieldClass.equals(Integer.class) || fieldClass.equals(int.class)) {
            return true;
        } else if (fieldClass.equals(Long.class) || fieldClass.equals(long.class)) {
            return true;
        } else if (fieldClass.equals(Float.class) || fieldClass.equals(float.class)) {
            return true;
        } else if (fieldClass.equals(Double.class) || fieldClass.equals(double.class)) {
            return true;
        } else if (fieldClass.equals(String.class)) {
            return true;
        } else if (fieldClass.equals(Boolean.class) || fieldClass.equals(boolean.class)) {
            return true;
        }

        // Non-primitive data
        try {
            field.setAccessible(true);
            Object destinationValue = field.get(obj);
            EntityObject destinationEntity =  db.entities.get(destinationValue.getClass());

            EntityManager em = KaolDB.getInstance().getEntityManager(context, db.name);
            QueryBuilder<?> qb = em.getQueryBuilder(destinationEntity.entityClass);
            Root<?> root = qb.getRoot(destinationEntity.entityClass, "de");

            Expression where = null;

            for (ColumnObject primaryKey : destinationEntity.primaryKeys) {
                if (primaryKey.field == null)
                    throw new InvalidConfigException("Primary key column " + primaryKey.name + " has null field");

                Property property = new Property<>(destinationEntity.entityClass, primaryKey.field.getType(), primaryKey.field.getName());
                Object value = primaryKey.field.get(destinationValue);
                Expression expression = root.eq(property, value);
                where = where == null ? expression : where.and(expression);
            }

            Object resultObj = qb.from(root).where(where).build("de").getSingleResult();
            return resultObj != null;

        } catch (IllegalAccessException e) {
            throw new QueryException(e.getMessage());
        }
    }


    /**
     * Insert field into content values
     *
     * @param   cv      content values
     * @param   obj     object containing the date
     * @param   db      database object
     * @param   field   column field
     */
    private static void insertFieldIntoContentValues(ContentValues cv, Object obj, DatabaseObject db, Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            insertColumnIntoContentValues(cv, obj, field, field.getAnnotation(Column.class));

        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            insertJoinColumnIntoContentValues(cv, obj, db, field, field.getAnnotation(JoinColumn.class));

        } else if (field.isAnnotationPresent(JoinColumns.class)) {
            insertJoinColumnsIntoContentValues(cv, obj, db, field, field.getAnnotation(JoinColumns.class));

        } else if (field.isAnnotationPresent(JoinTable.class)) {
            throw new QueryException("Not implemented");
        }
    }


    /**
     * Insert column field into content values
     *
     * @param   cv      content values
     * @param   obj     object containing the date
     * @param   field   column field
     */
    private static void insertColumnIntoContentValues(ContentValues cv, Object obj, Field field, Column annotation) {
        try {
            field.setAccessible(true);
            Object value = field.get(obj);
            insertDataIntoContentValues(cv, annotation.name(), value);

        } catch (IllegalAccessException e) {
            throw new QueryException(e.getMessage());
        }
    }


    /**
     * Insert join column field into content values
     *
     * @param   cv      content values
     * @param   obj     object containing the date
     * @param   db      database object
     * @param   field   column field
     */
    private static void insertJoinColumnIntoContentValues(ContentValues cv, Object obj, DatabaseObject db, Field field, JoinColumn annotation) {
        try {
            field.setAccessible(true);
            Object sourceObject = field.get(obj);

            if (sourceObject == null) {
                insertDataIntoContentValues(cv, annotation.name(), null);
            } else {
                EntityObject destinationEntity = db.entities.get(sourceObject.getClass());
                ColumnObject destinationColumn = destinationEntity.columnsNameMap.get(annotation.referencedColumnName());

                if (destinationColumn.field == null) return;
                destinationColumn.field.setAccessible(true);
                Object value = destinationColumn.field.get(sourceObject);

                insertDataIntoContentValues(cv, annotation.name(), value);
            }

        } catch (IllegalAccessException e) {
            throw new QueryException(e.getMessage());
        }
    }


    /**
     * Insert join columns field into content values
     *
     * @param   cv      content values
     * @param   obj     object containing the date
     * @param   db      database object
     * @param   field   column field
     */
    private static void insertJoinColumnsIntoContentValues(ContentValues cv, Object obj, DatabaseObject db, Field field, JoinColumns annotation) {
        for (JoinColumn joinColumnAnnotation : annotation.value()) {
            insertJoinColumnIntoContentValues(cv, obj, db, field, joinColumnAnnotation);
        }
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
