package it.mscuttari.kaoldb.core;

import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;

import static it.mscuttari.kaoldb.core.Constants.LOG_TAG;

class ColumnObject {

    // Class column field
    // Null if the discriminator column is not a class field
    @Nullable
    public Field field;

    // Annotation
    public Annotation annotation;

    // Column name
    public String name;

    // Column type
    // This is null until the entities relationships has not been checked
    public Class<?> type;

    // Nullable
    public boolean nullable;

    // Primary key
    public boolean primaryKey;

    // Unique
    public boolean unique;

    // Referenced column name (if join column)
    // Null if the column is not a join column
    @Nullable
    public String referencedColumnName;


    /**
     * Constructor
     *
     * @param   field       column field (must be annotated with @Column or @JoinColumn)
     */
    private ColumnObject(Field field) {
        this.field = field;
        this.name = getColumnName(field);
        this.primaryKey = field.isAnnotationPresent(Id.class);

        if (field.isAnnotationPresent(Column.class)) {
            this.annotation = field.getAnnotation(Column.class);
            this.type = field.getType();
            this.nullable = ((Column)annotation).nullable();
            this.unique = ((Column)annotation).unique();
            this.referencedColumnName = null;

        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            this.annotation = field.getAnnotation(JoinColumn.class);
            this.type = null;
            this.nullable = ((JoinColumn)annotation).nullable();
            this.unique = ((JoinColumn)annotation).unique();
            this.referencedColumnName = ((JoinColumn)annotation).referencedColumnName();
        }
    }


    /**
     * Constructor
     *
     * @param   joinColumnAnnotation    From column annotation (@JoinColumn)
     * @param   field                   field which has the above annotation
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
     * Convert column field to column object
     *
     * @param   field           column field
     * @return  column object
     */
    static ColumnObject columnFieldToColumnObject(Field field) {
        return new ColumnObject(field);
    }


    /**
     * Convert multiple From column field to a list of column objects
     *
     * @param   field       multiple From columns field
     * @return  list of column objects
     * @throws  InvalidConfigException if the field doesn't have the @JoinColumns annotation
     */
    static List<ColumnObject> joinColumnsFieldToColumnObjects(Field field) {
        List<ColumnObject> columns = new ArrayList<>();

        if (!field.isAnnotationPresent(JoinColumns.class))
            throw new InvalidConfigException("Field " + field.getName() + " doesn't have the @JoinColumns annotation");

        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);

        for (JoinColumn joinColumnAnnotation : joinColumnsAnnotation.value()) {
            columns.add(new ColumnObject(joinColumnAnnotation, field));
        }

        return columns;
    }


    /**
     * Convert From table field to a list of column objects
     *
     * @param   field       multiple From columns field
     * @return  list of column objects
     * @throws  InvalidConfigException if the field doesn't have the @JoinColumns annotation
     */
    static List<ColumnObject> joinTableFieldToColumnObjects(Field field) {
        List<ColumnObject> columns = new ArrayList<>();

        if (!field.isAnnotationPresent(JoinTable.class))
            throw new InvalidConfigException("Field " + field.getName() + " doesn't have the @JoinColumns annotation");

        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        for (JoinColumn joinColumnAnnotation : joinTableAnnotation.joinColumns()) {
            columns.add(new ColumnObject(joinColumnAnnotation, field));
        }

        return columns;
    }


    /**
     * Get column tableName
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
        Log.i(LOG_TAG, field.getName() + ": column tableName not specified, using the default one based on field tableName");

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
     * @param   entities        map of all entities
     * @throws  InvalidConfigException if the configuration is invalid
     */
    void checkConsistence(Map<Class<?>, EntityObject> entities) {
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
            throw new InvalidConfigException("Field " + this.field.getName() + " has no column annotation");
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

            String referencedColumnName = ((JoinColumn)this.annotation).referencedColumnName();
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