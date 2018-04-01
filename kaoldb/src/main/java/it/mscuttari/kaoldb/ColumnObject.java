package it.mscuttari.kaoldb;

import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

class ColumnObject {

    // Class column field
    public Field field;

    // Annotation
    public Annotation annotation;

    // Column name
    public String name;

    // Column type
    // This is null until the entities relationships has not been checked yet
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
     * @param   field       Field       column field (must be annotated with @Column or @JoinColumn)
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
     * @param   joinColumnAnnotation    JoinColumn      join column annotation (@JoinColumn)
     * @param   field                   Field           field which has the above annotation
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
        int hash = 7;
        int strlen = name == null ? 0 : name.length();

        for (int i = 0; i < strlen; i++) {
            hash = hash*31 + name.charAt(i);
        }

        return hash;
    }


    /**
     * Convert column field to column object
     *
     * @param   field       Field       column field
     * @return  column object
     */
    static ColumnObject columnFieldToColumnObject(Field field) {
        return new ColumnObject(field);
    }


    /**
     * Convert multiple join column field to a list of column objects
     *
     * @param   field       Field       multiple join columns field
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
     * Convert join table field to a list of column objects
     *
     * @param   field       Field       multiple join columns field
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
     * @param   field       Field       column field
     * @return  table tableName
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
     * @param   entities    List    list of all entities
     * @throws  InvalidConfigException if the configuration is invalid
     */
    void checkConsistence(List<EntityObject> entities) {
        // Check annotation count
        int annotationCount = 0;

        if (field.isAnnotationPresent(Column.class)) annotationCount++;
        if (field.isAnnotationPresent(JoinColumn.class)) annotationCount++;
        if (field.isAnnotationPresent(JoinColumns.class)) annotationCount++;
        if (field.isAnnotationPresent(JoinTable.class)) annotationCount++;

        if (annotationCount == 0) {
            throw new InvalidConfigException("Field " + field.getName() + " has no column annotation");
        } else if (annotationCount > 1) {
            throw new InvalidConfigException("Field " + field.getName() + " has too much column annotations");
        }


        // Check reference
        if (annotation instanceof JoinColumn) {
            // @JoinColumn
            Class<?> referencedClass = field.getType();
            EntityObject referencedEntity = null;

            for (EntityObject entity : entities) {
                if (entity.modelClass.equals(referencedClass)) {
                    referencedEntity = entity;
                    break;
                }
            }

            if (referencedEntity == null)
                throw new InvalidConfigException("Field " + field.getName() + ": " + referencedClass.getSimpleName() + " is not an entity");

            String referencedColumnName = ((JoinColumn)annotation).referencedColumnName();
            ColumnObject referencedColumn = referencedEntity.searchColumn(referencedColumnName, true);

            if (referencedColumn == null)
                throw new InvalidConfigException("Field " + field.getName() + ": referenced column " + referencedColumnName + " not found");

            type = referencedColumn.type;
        }

    }

}