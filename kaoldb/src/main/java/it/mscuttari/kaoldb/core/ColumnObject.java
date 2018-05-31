package it.mscuttari.kaoldb.core;

import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;

/**
 * Each {@link ColumnObject} maps a field annotated with {@link Column}, {@link JoinColumn},
 * {@link JoinColumns} or {@link JoinTable} annotations.
 */
class ColumnObject {

    /** Class column field */
    public Field field;

    /** Annotation of the field */
    public Annotation annotation;

    /** Column name */
    public String name;

    /**
     * Column type
     *
     * This is null until the entities relationships has not been checked with the
     * {@link EntityObject#checkConsistence(Map)} method
     */
    public Class<?> type;

    /** Nullable column property */
    public boolean nullable;

    /** Primary key column property */
    public boolean primaryKey;

    /** Unique column property */
    public boolean unique;

    /**
     * Referenced column name (if the data is represented by another entity)
     * Null if the column consist of basic data (string, integer, etc.)
     */
    @Nullable
    public String referencedColumnName;


    /**
     * Constructor for fields annotated with {@link Column}
     *
     * @param   columnAnnotation    {@link Column} annotation
     * @param   field               field which has the {@link Column} annotation
     */
    private ColumnObject(Column columnAnnotation, Field field) {
        this.field = field;
        this.name = getColumnName(field);
        this.primaryKey = field.isAnnotationPresent(Id.class);
        this.annotation = columnAnnotation;
        this.type = field.getType();
        this.nullable = columnAnnotation.nullable();
        this.unique = columnAnnotation.unique();
        this.referencedColumnName = null;
    }


    /**
     * Constructor for fields annotated with {@link JoinColumn}
     *
     * @param   joinColumnAnnotation    {@link JoinColumn} annotation
     * @param   field                   field which has the {@link JoinColumn} annotation
     */
    private ColumnObject(JoinColumn joinColumnAnnotation, Field field) {
        this.field = field;
        this.annotation = joinColumnAnnotation;
        this.name = joinColumnAnnotation.name();
        this.type = null;
        this.nullable = joinColumnAnnotation.nullable();
        this.primaryKey = field.isAnnotationPresent(Id.class);
        this.unique = joinColumnAnnotation.unique();
        this.referencedColumnName = joinColumnAnnotation.referencedColumnName();
    }


    @Override
    public String toString() {
        return name;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ColumnObject)) return false;
        ColumnObject columnObject = (ColumnObject)obj;
        if (name == null && columnObject.name == null) return true;
        if (name != null) return name.equals(columnObject.name);
        return false;
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
            // @JoinTable
            JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

            for (JoinColumn joinColumnAnnotation : joinTableAnnotation.joinColumns()) {
                result.add(new ColumnObject(joinColumnAnnotation, field));
            }
        } else {
            // No annotation found
            throw new InvalidConfigException("No column annotation found for field " + field.getName());
        }

        return result;
    }


    /**
     * Get column name
     *
     * If the table is not specified, the following policy is applied:
     * Uppercase characters are replaces with underscore followed by the same character converted to lowercase
     * Only the first class tableName character, if uppercase, is converted to lowercase avoiding the underscore
     * Example: columnFieldName => column_field_name
     *
     * @param   field       column field
     * @return  table name
     * @throws  InvalidConfigException if the field doesn't have @Column or @JoinColumn annotations
     */
    private static String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            // @Column
            Column column = field.getAnnotation(Column.class);
            if (!column.name().isEmpty()) return column.name();

        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            // @JoinColumn
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (!joinColumn.name().isEmpty()) return joinColumn.name();

        } else {
            throw new InvalidConfigException("Field " + field.getName() + " doesn't have @Column or @JoinColumn annotations");
        }

        // Currently not reachable (column name is a required field)
        LogUtils.w(field.getName() + ": column tableName not specified, using the default one based on field tableName");

        // Default table tableName
        String fieldName = field.getName();
        char c[] = fieldName.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        fieldName = new String(c);
        return fieldName.replaceAll("([A-Z])", "_$1").toLowerCase();
    }


    /**
     * Check column consistence
     *
     * Method called during the mapping consistence check
     * @see EntityObject#checkConsistence(Map)
     *
     * @param   entities        map of all entities
     * @throws  InvalidConfigException if the configuration is invalid
     */
    public void checkConsistence(Map<Class<?>, EntityObject> entities) {
        // Class doesn't have column field
        if (this.field == null)
            return;

        // Check annotation count
        int annotationCount = 0;

        if (this.field.isAnnotationPresent(Column.class)) annotationCount++;
        if (this.field.isAnnotationPresent(JoinColumn.class)) annotationCount++;
        if (this.field.isAnnotationPresent(JoinColumns.class)) annotationCount++;
        if (this.field.isAnnotationPresent(JoinTable.class)) annotationCount++;

        if (annotationCount == 0) {
            throw new InvalidConfigException("Field " + this.field.getName() + " has no @Column annotation");
        } else if (annotationCount > 1) {
            throw new InvalidConfigException("Field " + this.field.getName() + " has too much column annotations");
        }

        // Check references
        if (this.annotation instanceof JoinColumn) {
            // @JoinColumn
            Class<?> referencedClass = this.field.getType();
            EntityObject referencedEntity = entities.get(referencedClass);

            if (referencedEntity == null)
                throw new InvalidConfigException("Field " + this.field.getName() + ": " + referencedClass.getSimpleName() + " is not an entity");

            String referencedColumnName = ((JoinColumn) this.annotation).referencedColumnName();
            ColumnObject referencedColumn = null;

            while (referencedEntity != null && referencedColumn == null) {
                referencedColumn = referencedEntity.columnsNameMap.get(referencedColumnName);
                referencedEntity = referencedEntity.parent;
            }

            if (referencedColumn == null)
                throw new InvalidConfigException("Field " + field.getName() + ": referenced column " + referencedColumnName + " not found");

            this.type = referencedColumn.type;
        }
    }

}