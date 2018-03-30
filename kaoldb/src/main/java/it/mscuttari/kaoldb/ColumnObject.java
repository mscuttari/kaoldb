package it.mscuttari.kaoldb;

import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.KaolDBException;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

class ColumnObject {

    public Field field;             // Class column field
    public String name;             // Column name
    public Class<?> type;           // Column type
    public boolean nullable;        // Nullable
    public boolean primaryKey;      // Primary key
    public boolean unique;          // Unique


    /**
     * Constructor
     *
     * @param   field       Field       column field (must be annotated with @Column or @JoinColumn)
     */
    private ColumnObject(Field field) {
        this.field = field;
        this.name = getColumnName(field);
        this.type = getType(field);
        this.nullable = isNullable(field);
        this.primaryKey = isPrimaryKey(field);
        this.unique = isUnique(field);
    }


    /**
     * Constructor
     *
     * @param   joinColumnAnnotation    JoinColumn      join column annotation
     * @param   field                   Field           field which has the above annotation
     */
    private ColumnObject(JoinColumn joinColumnAnnotation, Field field) {
        this.field = field;
        this.name = joinColumnAnnotation.name();
        this.type = joinColumnAnnotation.type();
        this.nullable = joinColumnAnnotation.nullable();
        this.primaryKey = isPrimaryKey(field);
        this.unique = joinColumnAnnotation.unique();
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
     * @throws  KaolDBException if the field doesn't have the @JoinColumns annotation
     */
    static List<ColumnObject> joinColumnsFieldToColumnObjects(Field field) {
        List<ColumnObject> columns = new ArrayList<>();

        if (!field.isAnnotationPresent(JoinColumns.class))
            throw new KaolDBException("Field " + field.getName() + " doesn't have the @JoinColumns annotation");

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
     * @throws  KaolDBException if the field doesn't have the @JoinColumns annotation
     */
    static List<ColumnObject> joinTableFieldToColumnObjects(Field field) {
        List<ColumnObject> columns = new ArrayList<>();

        if (!field.isAnnotationPresent(JoinTable.class))
            throw new KaolDBException("Field " + field.getName() + " doesn't have the @JoinColumns annotation");

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
     * @throws  KaolDBException if the field doesn't have @Column or @JoinColumn annotations
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
            throw new KaolDBException("Field " + field.getName() + " doesn't have @Column or @JoinColumn annotations");
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
     * Get column type
     *
     * @param   field       Field       column field
     * @return  column type
     * @throws  KaolDBException if the field is not a column
     */
    private static Class<?> getType(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            // @Column
            return field.getType();

        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            // @JoinColumn
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            return joinColumn.type();
        }

        throw new KaolDBException("Field is not a column");
    }


    /**
     * Check if column is nullable
     *
     * @param   field       Field       column field
     * @return  true if nullable; false otherwise
     * @throws  KaolDBException if the field is not a column
     */
    private static boolean isNullable(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            // @Column
            return field.getAnnotation(Column.class).nullable();

        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            // @JoinColumn
            return field.getAnnotation(JoinColumn.class).nullable();
        }

        throw new KaolDBException("Field is not a column");
    }


    /**
     * Check if column is a primary key
     *
     * @param   field       Field       column field
     * @return  true if primary key; false otherwise
     */
    private static boolean isPrimaryKey(Field field) {
        return field.isAnnotationPresent(Id.class);
    }


    /**
     * Check if column is unique
     *
     * @param   field       Field       column field
     * @return  true if unique; false otherwise
     * @throws  KaolDBException if the field is not a column
     */
    private static boolean isUnique(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            // @Column
            return field.getAnnotation(Column.class).unique();

        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            // @JoinColumn
            return field.getAnnotation(JoinColumn.class).unique();
        }

        throw new KaolDBException("Field is not a column");
    }

}