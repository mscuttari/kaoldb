package it.mscuttari.kaoldb.core;

import android.support.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.exceptions.QueryException;

/**
 * Each {@link ColumnObject} maps a field annotated with {@link Column}, {@link JoinColumn},
 * {@link JoinColumns} or {@link JoinTable} annotations.
 */
class ColumnObject {

    /** Class column field */
    public Field field;

    /** Annotation of the column ({@link Column} or {@link JoinColumn}) */
    public Annotation columnAnnotation;

    /** Column name */
    public String name;

    /** Custom column definition */
    public String customColumnDefinition;

    /**
     * Column type
     *
     * This field may contain a wrong value until the entities relationships has not been
     * checked with the {@link EntityObject#checkConsistence(Map)} method
     */
    public Class<?> type;

    /** Nullable column property */
    public boolean nullable;

    /** Primary key column property */
    public boolean primaryKey;

    /** Unique column property */
    public boolean unique;

    /** Default value */
    public String defaultValue;


    /**
     * Constructor for fields annotated with {@link Column}
     *
     * @param   columnAnnotation    {@link Column} annotation
     * @param   field               field which has the {@link Column} annotation
     */
    private ColumnObject(Column columnAnnotation, Field field) {
        field.setAccessible(true);

        this.field = field;
        this.columnAnnotation = columnAnnotation;

        this.name = getName(field, columnAnnotation);
        this.customColumnDefinition = getCustomColumnDefinition(columnAnnotation);
        this.type = getFieldType(field);
        this.nullable = isNullable(field, columnAnnotation);
        this.primaryKey = isPrimaryKey(field);
        this.unique = columnAnnotation.unique();
        this.defaultValue = getDefaultValue(columnAnnotation);
    }


    /**
     * Constructor for fields annotated with {@link JoinColumn}
     *
     * @param   joinColumnAnnotation    {@link JoinColumn} annotation
     * @param   field                   field which has the {@link JoinColumn} annotation
     */
    private ColumnObject(JoinColumn joinColumnAnnotation, Field field) {
        field.setAccessible(true);

        this.field = field;
        this.columnAnnotation = joinColumnAnnotation;

        this.name = getName(field, joinColumnAnnotation);
        this.customColumnDefinition = getCustomColumnDefinition(joinColumnAnnotation);
        this.type = getFieldType(field);
        this.nullable = isNullable(field, joinColumnAnnotation);
        this.primaryKey = isPrimaryKey(field);
        this.unique = joinColumnAnnotation.unique();
        this.defaultValue = getDefaultValue(joinColumnAnnotation);
    }


    @Override
    public String toString() {
        return name;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ColumnObject))
            return false;

        ColumnObject columnObject = (ColumnObject) obj;

        return (name == null && columnObject.name == null) ||
                (name != null && name.equals(columnObject.name));
    }


    @Override
    public int hashCode() {
        return this.name.hashCode();
    }


    /**
     * Convert column field to single or multiple column objects
     *
     * Fields annotated with {@link Column} or {@link JoinColumn} will lead to a
     * {@link java.util.Collection} populated with just one element.
     * Fields annotated with {@link JoinColumns} or {@link JoinTable} will lead to a
     * {@link java.util.Collection} populated with multiple elements according to the join
     * columns number
     *
     * @param   field           class field
     * @return  column objects collection
     * @throws  InvalidConfigException if there is no column annotation
     */
    public static Collection<ColumnObject> fieldToObject(Field field) {
        Collection<ColumnObject> result = new HashSet<>();

        if (field.isAnnotationPresent(Column.class)) {
            // @Column
            result.add(new ColumnObject(field.getAnnotation(Column.class), field));

        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            // @JoinColumn
            result.add(new ColumnObject(field.getAnnotation(JoinColumn.class), field));

        } else if (field.isAnnotationPresent(JoinColumns.class)) {
            // @JoinColumns
            JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);

            for (JoinColumn joinColumnAnnotation : joinColumnsAnnotation.value()) {
                result.add(new ColumnObject(joinColumnAnnotation, field));
            }

        } else if (field.isAnnotationPresent(JoinTable.class)) {
            // Fields annotated with @JoinTable are skipped because they don't lead to new columns.
            // In fact, the annotation should only map the existing table columns to the join table
            // ones, which are created separately

            return result;

        } else {
            // No annotation found
            throw new InvalidConfigException("No column annotation found for field " + field.getName());
        }

        return result;
    }


    /**
     * Get the columns of a join table
     *
     * All the entities must be already mapped
     *
     * @param   db          database object
     * @param   field       field owning the join table annotation
     *
     * @return  columns of a join table
     */
    public static Collection<ColumnObject> getJoinTableColumns(DatabaseObject db, Field field) {
        Collection<ColumnObject> result = new HashSet<>();
        JoinTable annotation = field.getAnnotation(JoinTable.class);
        if (annotation == null) return result;  // Just a security check

        // Direct join columns
        for (JoinColumn joinColumn : annotation.joinColumns()) {
            result.add(new ColumnObject(joinColumn, field));
        }

        // Inverse join columns
        for (JoinColumn inverseJoinColumn : annotation.inverseJoinColumns()) {
            result.add(new ColumnObject(inverseJoinColumn, field));
        }

        // Fix columns types
        for (ColumnObject column : result) {
            column.fixType(db.getEntitiesMap());
        }

        return result;
    }


    /**
     * Get column name of a field annotated with {@link Column} or {@link JoinColumn}
     *
     * If the {@link Column#name()} or the {@link JoinColumn#name()} are not specified, the
     * following policy is applied:
     *
     * Uppercase characters are replaced with underscore followed by the same character converted
     * to lowercase. Only the first character, if uppercase, is converted to lowercase avoiding
     * the underscore.
     *
     * Example: columnFieldName => column_field_name
     *
     * @param   field               column field
     * @param   columnAnnotation    {@link Column} / {@link JoinColumn} annotation of the field
     *
     * @return  column name
     */
    private static String getName(Field field, Annotation columnAnnotation) {
        if (columnAnnotation instanceof Column) {
            Column column = (Column) columnAnnotation;
            if (!column.name().isEmpty()) return column.name();

        } else if (columnAnnotation instanceof JoinColumn) {
            JoinColumn joinColumn = (JoinColumn) columnAnnotation;

            if (!joinColumn.name().isEmpty())
                return joinColumn.name();
        }

        LogUtils.w(field.getName() + ": column name not specified, using the default one based on field name");

        // Default column name
        String fieldName = field.getName();
        char c[] = fieldName.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        fieldName = new String(c);
        return fieldName.replaceAll("([A-Z])", "_$1").toLowerCase();
    }


    /**
     * Get custom column definition to be used during the table creation phase
     *
     * @param   columnAnnotation        {@link Column} / {@link JoinColumn} annotation of the field
     * @return  SQL statement (null if there is no custom column definition)
     */
    @Nullable
    private static String getCustomColumnDefinition(Annotation columnAnnotation) {
        if (columnAnnotation instanceof Column) {
            Column annotation = (Column) columnAnnotation;
            return annotation.columnDefinition().isEmpty() ? null : annotation.columnDefinition();

        } else if (columnAnnotation instanceof JoinColumn) {
            JoinColumn annotation = (JoinColumn) columnAnnotation;
            return annotation.columnDefinition().isEmpty() ? null : annotation.columnDefinition();
        }

        return null;
    }


    /**
     * Get field type
     *
     * If the field is annotated with {@link OneToMany} or {@link ManyToMany}, the returned class
     * is the collection elements one
     *
     * @param   field   field
     * @return  field type
     */
    public static Class<?> getFieldType(Field field) {
        if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
            ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
            return (Class<?>) collectionType.getActualTypeArguments()[0];
        }

        return field.getType();
    }


    /**
     * Check whether the column is nullable or not
     *
     * @param   field               column field
     * @param   columnAnnotation    {@link Column} / {@link JoinColumn} annotation of the field
     *
     * @return  true if the column is nullable; false otherwise
     */
    private static boolean isNullable(Field field, Annotation columnAnnotation) {
        if (columnAnnotation instanceof Column) {
            // Normal columns are nullable only if directly specified
            return ((Column) columnAnnotation).nullable();

        } else if (columnAnnotation instanceof JoinColumn) {
            JoinColumn joinColumnAnnotation = (JoinColumn) columnAnnotation;

            // If the columns is nullable by itself, then the relationship optionality doesn't matter
            if (joinColumnAnnotation.nullable())
                return true;

            // A non nullable may be null if the relationship is optional
            if (field.isAnnotationPresent(OneToOne.class)) {
                return field.getAnnotation(OneToOne.class).optional();
            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                return field.getAnnotation(ManyToOne.class).optional();
            }
        }

        // Normally not reachable
        return false;
    }


    /**
     * Check whether the column is a primary key for the table it belongs to
     *
     * It can be considered as a primary key if the field is annotated with {@link Id} (explicit
     * primary key declaration) or if it is annotated with {@link JoinTable} (all the join table
     * columns must be primary keys).
     *
     * @param   field       column field
     * @return  true if the column is a primary key; false otherwise
     */
    private static boolean isPrimaryKey(Field field) {
        return field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(JoinTable.class);
    }


    /**
     * Get column default value to be used during the table creation phase
     *
     * @param   columnAnnotation        {@link Column} / {@link JoinColumn} annotation of the field
     * @return  SQL statement (null if there is no custom column definition)
     */
    @Nullable
    private static String getDefaultValue(Annotation columnAnnotation) {
        if (columnAnnotation instanceof Column) {
            String defaultValue = ((Column) columnAnnotation).defaultValue();
            return defaultValue.isEmpty() ? null : defaultValue;

        } else if (columnAnnotation instanceof JoinColumn) {
            String defaultValue = ((JoinColumn) columnAnnotation).defaultValue();
            return defaultValue.isEmpty() ? null : defaultValue;
        }

        return null;
    }


    /**
     * Fix column name and type according to its origin
     *
     * This method called during the mapping consistence check
     *
     * @see EntityObject#checkConsistence(Map)
     *
     * @param   entities        map of all entities
     * @throws  InvalidConfigException if the configuration is invalid
     */
    public void fixType(Map<Class<?>, EntityObject> entities) {
        // Class doesn't have column field
        if (this.field == null)
            return;

        // Determine the correct column type

        if (!(this.columnAnnotation instanceof JoinColumn)) {
            // No need if the column is not used to join two entities
            return;
        }

        Class<?> referencedClass = null;
        String referencedColumnName = null;

        if (this.field.isAnnotationPresent(JoinColumn.class) || this.field.isAnnotationPresent(JoinColumns.class)) {
            // The linked column directly belongs to the linked entity class, so to determine the
            // column type is sufficient to search for that foreign key and get its type

            referencedColumnName = ((JoinColumn) this.columnAnnotation).referencedColumnName();
            referencedClass = this.type;

        } else if (this.field.isAnnotationPresent(JoinTable.class)) {
            // The linked column belongs to a middle join table, so to determine the column type
            // we need to first determine that column class. Its class is indeed derived from the
            // foreign key of the second entity

            referencedColumnName = ((JoinColumn) this.columnAnnotation).referencedColumnName();
            JoinTable joinTableAnnotation = this.field.getAnnotation(JoinTable.class);

            // We need to determine if the column is a direct or inverse join column.
            // If it is a direct join column, the column type is the same of the field enclosing class.
            // If it is an inverse join column, the column type is the field one.

            boolean direct = false;

            // Search in the direct join columns.
            // If not found, then the column must be an inverse join one.

            for (JoinColumn joinColumn : joinTableAnnotation.joinColumns()) {
                if (joinColumn.equals(this.columnAnnotation)) {
                    direct = true;
                    break;
                }
            }

            if (direct) {
                referencedClass = this.field.getDeclaringClass();
            } else {
                referencedClass = getFieldType(this.field);
            }
        }

        if (referencedClass == null) {
            // Normally not reachable. Just a security check
            throw new InvalidConfigException("Field \"" + this.field.getName() + "\": can't determine the referenced class");
        }

        // Search the referenced column
        EntityObject referencedEntity = entities.get(referencedClass);

        if (referencedEntity == null)
            throw new InvalidConfigException("Field \"" + this.field.getName() + "\": \"" + referencedClass.getSimpleName() + "\" is not an entity");

        ColumnObject referencedColumn = null;

        while (referencedEntity != null && referencedColumn == null) {
            referencedColumn = referencedEntity.columnsNameMap.get(referencedColumnName);
            referencedEntity = referencedEntity.parent;
        }

        if (referencedColumn == null)
            throw new InvalidConfigException("Field \"" + this.field.getName() + "\": referenced column \"" + referencedColumnName + "\" not found");

        this.type = referencedColumn.type;
    }


    /**
     * Get field value
     *
     * @param   obj     object to get the value from
     * @param   <T>     object class
     *
     * @return  field value
     *
     * @throws  QueryException if the field can't be accessed
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(Object obj) {
        // Just a security check
        if (field == null)
            return null;

        field.setAccessible(true);

        try {
            return (T) field.get(obj);
        } catch (IllegalAccessException e) {
            throw new QueryException(e);
        }
    }


    /**
     * Set field value
     *
     * @param   obj     object containing the field to be set
     * @param   value   value to be set
     *
     * @throws  QueryException if the field can't be accessed
     */
    public void setValue(Object obj, Object value) {
        field.setAccessible(true);

        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new QueryException(e);
        }
    }

}