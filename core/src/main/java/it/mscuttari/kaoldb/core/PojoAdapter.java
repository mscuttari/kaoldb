package it.mscuttari.kaoldb.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.exceptions.PojoException;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.EntityManager;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

/**
 * Utility class that allows to convert the query results into POJOs (Plain Old Java Object) and
 * POJOs into data ready to be stored.
 *
 * @see #cursorToObject(DatabaseObject, Cursor, Map, Class, String) for Cursor => POJO
 * @see #objectToContentValues(Context, DatabaseObject, EntityObject, EntityObject, Object) for POJO => ContentValues
 */
class PojoAdapter {

    private PojoAdapter() {

    }


    /**
     * Convert cursor to POJO
     *
     * The conversion automatically search for the child class according to the discriminator value.
     * In fact, the method starts with a first part with the aim of going down through the hierarchy
     * tree in order to retrieve the leaf class representing the real object class.
     * Then, the second part creates an instance of that class and populates its basic fields with
     * the data contained in the cursor.
     *
     * @param db            database object
     * @param c             cursor
     * @param cursorMap     map between cursor column names and column indexes
     *                      (for more details see {@link QueryImpl#getCursorColumnMap(Cursor)})
     * @param resultClass   desired result class (if it has children, it will be just a super
     *                      class of the result object)
     *
     * @return populated object
     *
     * @throws PojoException if the child class is not found (wrong discriminator column value) or
     *                       if it can not be instantiated
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T cursorToObject(DatabaseObject db, Cursor c, Map<String, Integer> cursorMap, Class<T> resultClass, String alias) {
        // Starting entity
        EntityObject<?> entity = db.getEntity(resultClass);

        // Go down to the child class.
        // Each iteration will go one step down through the hierarchy tree.

        while (entity.children.size() != 0) {
            // Get the discriminator value
            String discriminatorColumnName = alias + entity.clazz.getSimpleName() + "." + entity.discriminatorColumn.name;
            int columnIndex = cursorMap.get(discriminatorColumnName);
            Object discriminatorValue = null;

            if (entity.discriminatorColumn.type.equals(Integer.class)) {
                discriminatorValue = c.getInt(columnIndex);
            } else if (entity.discriminatorColumn.type.equals(String.class)) {
                discriminatorValue = c.getString(columnIndex);
            }

            // Determine the child class according to the discriminator value specified
            boolean childClassFound = false;

            for (EntityObject child : entity.children) {
                // Just a security check
                if (child.discriminatorValue == null)
                    continue;

                // Comparison with the child class discriminator value
                if (child.discriminatorValue.equals(discriminatorValue)) {
                    childClassFound = true;
                    entity = child;
                    break;
                }
            }

            if (!childClassFound)
                throw new PojoException("Child class not found");
        }

        // Just a security check
        if (!resultClass.isAssignableFrom(entity.clazz))
            throw new PojoException("Result class " + resultClass.getSimpleName() + " requested but the entity object contains class " +  entity.getName());

        // Populate child class
        try {
            T result = (T) entity.clazz.newInstance();

            while (entity != null) {
                if (entity.realTable) {
                    for (BaseColumnObject column : entity.columns) {
                        if (column.field == null) continue;

                        Object value;

                        if (!column.field.isAnnotationPresent(OneToOne.class) &&
                                !column.field.isAnnotationPresent(OneToMany.class) &&
                                !column.field.isAnnotationPresent(ManyToOne.class) &&
                                !column.field.isAnnotationPresent(ManyToMany.class)) {

                            String columnName = alias + entity.getName() + "." + column.name;
                            value = cursorFieldToObject(c, cursorMap, columnName, column.field.getType());

                        } else {
                            // Relationships are loaded separately
                            value = null;
                        }

                        column.field.setAccessible(true);
                        column.field.set(result, value);
                    }
                }

                entity = entity.parent;
            }

            return result;

        } catch (InstantiationException e) {
            throw new PojoException(e);

        } catch (IllegalAccessException e) {
            throw new PojoException(e);
        }
    }


    /**
     * Convert {@link Cursor} field to object
     *
     * @param c             cursor
     * @param cursorMap     map between cursor column names and column indexes
     * @param columnName    column name
     *
     * @return column value
     *
     * @throws PojoException if the data type is not compatible with the one found in the database
     */
    @SuppressWarnings("unchecked")
    private static <T> T cursorFieldToObject(Cursor c, Map<String, Integer> cursorMap, String columnName, Class<T> dataType) {
        int columnIndex = cursorMap.get(columnName);
        int columnType = c.getType(columnIndex);

        Object value = null;

        if (columnType == Cursor.FIELD_TYPE_INTEGER) {
            if (!(dataType.equals(Integer.class) || dataType.equals(int.class) || !dataType.equals(Date.class) || !dataType.equals(Calendar.class)))
                throw new PojoException("Incompatible data type: expected " + dataType.getSimpleName() + ", found Integer");

            if (dataType.equals(Integer.class) || dataType.equals(int.class)) {
                value = c.getInt(columnIndex);

            } else if (dataType.equals(Long.class) || dataType.equals(long.class)) {
                value = c.getLong(columnIndex);

            } else if (dataType.equals(Date.class)) {
                value = new Date(c.getLong(columnIndex));

            } else if (dataType.equals(Calendar.class)) {
                value = Calendar.getInstance();
                ((Calendar) value).setTimeInMillis(c.getLong(columnIndex));
            }

        } else if (columnType == Cursor.FIELD_TYPE_FLOAT) {
            if (!(dataType.equals(Float.class) || dataType.equals(float.class) || dataType.equals(Double.class) || dataType.equals(double.class)))
                throw new PojoException("Incompatible data type: expected " + dataType.getSimpleName() + ", found Float");

            if (dataType.equals(Float.class) || dataType.equals(float.class)) {
                value = c.getFloat(columnIndex);

            } else if (dataType.equals(Double.class) || dataType.equals(double.class)) {
                value = c.getDouble(columnIndex);
            }

        } else if (columnType == Cursor.FIELD_TYPE_STRING) {
            if (dataType.isEnum()) {
                Enum enumClass = Enum.class.cast(dataType);
                value = Enum.valueOf(enumClass.getClass(), c.getString(columnIndex));
                
            } else if (String.class.isAssignableFrom(dataType)) {
                value = c.getString(columnIndex);
            }

            if (!(dataType.equals(String.class)))
                throw new PojoException("Incompatible data type: expected " + dataType.getSimpleName() + ", found String");

        }

        return (T) value;
    }


    /**
     * Prepare the data to be saved in a particular entity table
     *
     * {@link ContentValues#size()} must be checked before saving the data in the database.
     * If zero, no data needs to be saved and, if not skipped, the
     * {@link SQLiteDatabase#insert(String, String, ContentValues)} method would throw an exception.
     *
     * @param context           context
     * @param db                database object
     * @param currentEntity     current entity object
     * @param childEntity       child entity object
     * @param obj               object to be persisted
     *
     * @return data ready to be saved in the database
     *
     * @throws QueryException  if the discriminator value has been manually set but is not
     *                         compatible with the child entity class
     * @throws QueryException  if the field associated with the discriminator value can't be accessed
     */
    public static ContentValues objectToContentValues(Context context, DatabaseObject db, EntityObject currentEntity, EntityObject childEntity, Object obj) {
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

                if (specifiedDiscriminatorValue == null || !specifiedDiscriminatorValue.equals(childEntity.discriminatorValue))
                    throw new QueryException("Wrong discriminator value: expected " + childEntity.discriminatorValue + ", found " + specifiedDiscriminatorValue);

            } else {
                // The discriminator value has not been found. Adding it automatically

                if (currentEntity.discriminatorColumn.field.isAnnotationPresent(Column.class)) {
                    // The discriminator column is linked to a field annotated with @Column
                    insertDataIntoContentValues(cv, currentEntity.discriminatorColumn.name, childEntity.discriminatorValue);

                } else if (currentEntity.discriminatorColumn.field.isAnnotationPresent(JoinColumn.class)) {
                    // The discriminator column is linked to a field annotated with @JoinColumn
                    JoinColumn joinColumnAnnotation = currentEntity.discriminatorColumn.field.getAnnotation(JoinColumn.class);
                    Class<?> discriminatorType = currentEntity.discriminatorColumn.field.getType();
                    Object discriminator;

                    try {
                        // Create the discriminator object containing the discriminator column
                        discriminator = discriminatorType.newInstance();

                        // Set the child discriminator value
                        Field discriminatorField = discriminatorType.getField(joinColumnAnnotation.referencedColumnName());
                        discriminatorField.set(discriminator, childEntity.discriminatorValue);

                        // Assign the discriminator value to the object to be persisted
                        currentEntity.discriminatorColumn.field.set(obj, discriminator);

                    } catch (InstantiationException e) {
                        throw new QueryException(e);

                    } catch (IllegalAccessException e) {
                        throw new QueryException(e);

                    } catch (NoSuchFieldException e) {
                        throw new QueryException(e);
                    }

                    currentEntity.discriminatorColumn.addToContentValues(cv, obj);

                } else if (currentEntity.discriminatorColumn.field.isAnnotationPresent(JoinColumns.class)) {
                    // The discriminator column is linked to a field annotated with @JoinColumns
                    JoinColumns joinColumnsAnnotation = currentEntity.discriminatorColumn.field.getAnnotation(JoinColumns.class);
                    Class<?> discriminatorType = currentEntity.discriminatorColumn.field.getType();
                    Object discriminator;

                    try {
                        // Create the discriminator object containing the discriminator column
                        discriminator = discriminatorType.newInstance();

                        for (JoinColumn joinColumn : joinColumnsAnnotation.value()) {
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
                        throw new QueryException(e);

                    } catch (IllegalAccessException e) {
                        throw new QueryException(e);

                    } catch (NoSuchFieldException e) {
                        throw new QueryException(e);
                    }

                    currentEntity.discriminatorColumn.addToContentValues(cv, obj);

                } else if (currentEntity.discriminatorColumn.field.isAnnotationPresent(JoinTable.class)) {
                    // The discriminator column is linked to a field annotated with @JoinTable
                    Class<?> discriminatorType = currentEntity.discriminatorColumn.field.getType();
                    Object discriminator;

                    try {
                        // Create the discriminator object containing the discriminator column
                        discriminator = discriminatorType.newInstance();

                        JoinTable discriminatorColumnAnnotation = currentEntity.discriminatorColumn.field.getAnnotation(JoinTable.class);
                        for (JoinColumn joinColumn : ((JoinTable)discriminatorColumnAnnotation).joinColumns()) {
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
                        throw new QueryException(e);

                    } catch (IllegalAccessException e) {
                        throw new QueryException(e);

                    } catch (NoSuchFieldException e) {
                        throw new QueryException(e);
                    }

                    currentEntity.discriminatorColumn.addToContentValues(cv, obj);
                }
            }
        }


        // Columns
        for (BaseColumnObject column : currentEntity.columns) {
            Object fieldValue = column.getValue(obj);

            if (!checkDataExistence(fieldValue, context, db))
                throw new QueryException("Field \"" + column.field.getName() + "\" doesn't exist in the database. Persist it first!");

            column.addToContentValues(cv, obj);
        }

        return cv;
    }


    /**
     * Check if an object already exists in the database
     *
     * @param obj       {@link Object} to be searched. In case of basic object type (Integer,
     *                  String, etc.) the check will be successful; in case of complex type
     *                  (custom classes), a query searching for the object is run
     * @param context   application {@link Context}
     * @param db        {@link DatabaseObject} of the database the entity belongs to
     *
     * @return true if the data already exits in the database; false otherwise
     *
     * @throws InvalidConfigException  if the entity has a primary key not linked to a class field
     *                                 (not normally reachable situation)
     */
    private static boolean checkDataExistence(Object obj, Context context, DatabaseObject db) {
        // Null data is considered to be existing
        if (obj == null)
            return true;

        Class<?> clazz = obj.getClass();

        // Primitive data can't be stored alone in the database because they have no table associated.
        // Therefore, primitive data existence check should be skipped (this way it is always
        // considered successful)

        if (isPrimitiveType(clazz))
            return true;


        // Non-primitive data existence must be checked and so a select query is executed
        // The select query is based on the primary keys of the entity
        EntityObject entity = db.getEntity(clazz);

        EntityManager em = KaolDB.getInstance().getEntityManager(context, db.getName());
        QueryBuilder<?> qb = em.getQueryBuilder(entity.clazz);
        Root<?> root = qb.getRoot(entity.clazz, "de");

        Expression where = null;

        for (BaseColumnObject primaryKey : entity.columns.getPrimaryKeys()) {
            // Create the property that the generated class would have because of the primary key field
            SingleProperty property = new SingleProperty<>(entity.clazz, primaryKey.field.getType(), primaryKey.field);

            Object value = primaryKey.getValue(obj);
            Expression expression = root.eq(property, value);

            // In case of multiple primary keys, concatenated the WHERE expressions
            where = where == null ? expression : where.and(expression);
        }

        // Run the query and check if its result is not an empty set
        Object queryResult = qb.from(root).where(where).build("de").getSingleResult();
        return queryResult != null;
    }


    /**
     * Insert value into {@link ContentValues}
     *
     * The conversion of the object value to column value follows these rules:
     *  -   If the column name is already in use, its value is replaced with the newer one.
     *  -   Passing a null value will result in a NULL table column.
     *  -   An empty string ("") will not generate a NULL column but a string with a length of
     *      zero (empty string).
     *  -   Boolean values are stored either as 1 (true) or 0 (false).
     *  -   {@link Date} and {@link Calendar} objects are stored by saving their time in milliseconds.
     *
     * @param cv            content values
     * @param columnName    column name
     * @param value         value
     */
    public static void insertDataIntoContentValues(ContentValues cv, String columnName, Object value) {
        if (value == null) {
            cv.putNull(columnName);

        } else {
            if (value instanceof Enum) {
                cv.put(columnName, ((Enum) value).name());

            } else if (value instanceof Integer || value.getClass().equals(int.class)) {
                cv.put(columnName, (int) value);

            } else if (value instanceof Long || value.getClass().equals(long.class)) {
                cv.put(columnName, (long) value);

            } else if (value instanceof Float || value.getClass().equals(float.class)) {
                cv.put(columnName, (float) value);

            } else if (value instanceof Double || value.getClass().equals(double.class)) {
                cv.put(columnName, (double) value);

            } else if (value instanceof String) {
                cv.put(columnName, (String) value);

            } else if (value instanceof Boolean || value.getClass().equals(boolean.class)) {
                cv.put(columnName, ((boolean) value) ? 1 : 0);

            } else if (value instanceof Date) {
                cv.put(columnName, ((Date) value).getTime());

            } else if (value instanceof Calendar) {
                cv.put(columnName, ((Calendar) value).getTimeInMillis());
            }
        }
    }


    /**
     * Check if the data class is a primitive one (int, float, etc.) or if is one of the following:
     * {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link String},
     * {@link Boolean}. {@link Date}, {@link Calendar}.
     *
     * @param clazz     data class
     * @return true if one the specified classes is found
     */
    private static boolean isPrimitiveType(Class<?> clazz) {
        Class<?>[] primitiveTypes = {
                Enum.class,
                Integer.class, int.class,
                Long.class, long.class,
                Float.class, float.class,
                Double.class, double.class,
                String.class,
                Boolean.class, boolean.class,
                Date.class,
                Calendar.class
        };

        for (Class<?> primitiveType : primitiveTypes) {
            if (primitiveType.isAssignableFrom(clazz))
                return true;
        }

        return false;
    }

}
