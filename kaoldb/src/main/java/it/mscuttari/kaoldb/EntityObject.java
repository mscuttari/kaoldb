package it.mscuttari.kaoldb;

import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Inheritance;
import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.Table;
import it.mscuttari.kaoldb.annotations.UniqueConstraint;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;

import static it.mscuttari.kaoldb.Constants.LOG_TAG;

class EntityObject {

    public Class<?> modelClass;                 // Model class
    public String tableName;                    // Table name
    public List<ColumnObject> columns;          // Table columns
    public InheritanceType inheritanceType;     // Inheritance type
    public EntityObject parent;                 // Parent
    public List<EntityObject> children;         // Children


    /**
     * Constructor
     *
     * @param   modelClass      model class
     */
    private EntityObject(Class<?> modelClass) {
        this.modelClass = modelClass;
        this.tableName = getTableName(modelClass);
        this.columns = null;
        this.inheritanceType = getInheritanceType(modelClass);
        this.parent = null;
        this.children = new ArrayList<>();
    }


    @Override
    public String toString() {
        String result = "";

        result += "Class: " + modelClass.getSimpleName() + ", ";

        if (parent != null)
            result += "Parent: " + parent.modelClass.getSimpleName() + ", ";

        //result += "Children: " + children.toString() + ", ";
        result += "Columns: " + getColumns() + ", ";
        result += "Primary keys: " + getPrimaryKeys();

        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof EntityObject)) return false;
        EntityObject columnObject = (EntityObject)obj;
        if (modelClass == null && columnObject.modelClass == null) return true;
        if (modelClass != null) return modelClass.equals(columnObject.modelClass);
        return false;
    }


    @Override
    public int hashCode() {
        int hash = 7;
        String modelName = modelClass.getSimpleName();
        int strlen = modelName == null ? 0 : modelName.length();

        for (int i = 0; i < strlen; i++) {
            hash = hash*31 + modelName.charAt(i);
        }

        return hash;
    }


    /**
     * Get table object given model class
     *
     * @param   modelClass      Class       model class
     * @return  table object
     */
    static EntityObject modelClassToTableObject(Class<?> modelClass) {
        return new EntityObject(modelClass);
    }


    /**
     * Get table name
     *
     * If the table is not specified, the following policy is applied:
     * Uppercase characters are replaces with underscore followed by the same character converted to lowercase
     * Only the first class tableName character, if uppercase, is converted to lowercase avoiding the underscore
     * Example: ModelClassName => model_class_name
     *
     * @param   modelClass      Class       model class
     * @return  table tableName (null if the class doesn't need a real table)
     * @throws  InvalidConfigException if the class doesn't have the @Table annotation and the inheritance type is not TABLE_PER_CLASS
     */
    private String getTableName(Class<?> modelClass) {
        if (!modelClass.isAnnotationPresent(Table.class)) {
            if (isRealTable()) {
                throw new InvalidConfigException("Class " + modelClass.getSimpleName() + " doesn't have the @Table annotation");
            } else {
                return null;
            }
        }

        // Get specified table tableName
        Table table = modelClass.getAnnotation(Table.class);
        if (!table.name().isEmpty()) return table.name();
        Log.i(LOG_TAG, modelClass.getSimpleName() + ": table tableName not specified, using the default one based on class tableName");

        // Default table tableName
        String className = modelClass.getSimpleName();
        char c[] = className.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        className = new String(c);
        return className.replaceAll("([A-Z])", "_$1").toLowerCase();
    }


    /**
     * Get inheritance type
     *
     * @param   modelClass      Class       model class
     * @return  inheritance type
     */
    @Nullable
    private static InheritanceType getInheritanceType(Class<?> modelClass) {
        if (!modelClass.isAnnotationPresent(Inheritance.class))
            return null;

        Inheritance inheritance = modelClass.getAnnotation(Inheritance.class);
        return inheritance.strategy();
    }


    /**
     * Determine if the class is linked to a real table
     *
     * @return  boolean     true if a table should exists for this class; false if not
     * @throws  KaolDBException if a newly implemented inheritance type is not taken into consideration
     */
    boolean isRealTable() {
        InheritanceType inheritanceType = getInheritanceType(modelClass);

        if (inheritanceType == null) {
            return parent == null || parent.inheritanceType != InheritanceType.SINGLE_TABLE;
        }

        switch (inheritanceType) {
            case TABLE_PER_CLASS:
                return false;

            case SINGLE_TABLE:
            case JOINED:
                Class<?> parentClass = modelClass.getSuperclass();
                InheritanceType parentInheritancetype = getInheritanceType(parentClass);
                return parentInheritancetype != InheritanceType.SINGLE_TABLE;
        }

        throw new KaolDBException("Inheritance type not found");
    }


    /**
     * Get the fields with a specific annotation
     *
     * @param   fields              List        fields list to search in
     * @param   annotationClass     Class       desired annotation class
     *
     * @return  list of fields with the annotation specified
     */
    private static List<Field> getFieldsWithAnnotation(List<Field> fields, Class<? extends Annotation> annotationClass) {
        List<Field> result = new ArrayList<>(fields);

        for (Iterator<Field> it = result.iterator(); it.hasNext(); ) {
            Field field = it.next();
            field.setAccessible(true);

            if (!field.isAnnotationPresent(annotationClass))
                it.remove();
        }

        return result;
    }


    /**
     * Search and assign parent and children
     *
     * @param   entities    List    list of all entities
     */
    void searchParentAndChildren(List<EntityObject> entities) {
        for (EntityObject entity : entities) {
            // Parent
            if (this.modelClass.getSuperclass().equals(entity.modelClass)) {
                parent = entity;
            }

            // Children
            if (this.modelClass.equals(entity.modelClass.getSuperclass())) {
                children.add(entity);
            }
        }
    }


    /**
     * Load columns
     *
     * @return  list of columns
     * @throws  InvalidConfigException in case of multiple column declaration
     */
    List<ColumnObject> getColumns() {
        // Check if the columns have already been assigned
        if (this.columns != null)
            return this.columns;

        // Determine columns for the first time
        List<ColumnObject> columns = new ArrayList<>();
        List<Field> allFields = Arrays.asList(modelClass.getDeclaredFields());

        // @Column
        List<Field> columnFields = getFieldsWithAnnotation(allFields, Column.class);

        for (Field columnField : columnFields) {
            ColumnObject column = ColumnObject.columnFieldToColumnObject(columnField);
            if (columns.contains(column)) throw new InvalidConfigException("Column " + column.name + " already defined");
            columns.add(column);
        }

        // @JoinColumn
        List<Field> joinColumnFields = getFieldsWithAnnotation(allFields, JoinColumn.class);

        for (Field joinColumnField : joinColumnFields) {
            ColumnObject joinColumn = ColumnObject.columnFieldToColumnObject(joinColumnField);
            if (columns.contains(joinColumn)) throw new InvalidConfigException("Column " + joinColumn.name + " already defined");
            columns.add(joinColumn);
        }

        // @JoinColumns
        List<Field> joinColumnsFields = getFieldsWithAnnotation(allFields, JoinColumns.class);

        for (Field joinColumnsField : joinColumnsFields) {
            List<ColumnObject> joinColumns = ColumnObject.joinColumnsFieldToColumnObjects(joinColumnsField);

            for (ColumnObject joinColumn : joinColumns) {
                if (columns.contains(joinColumn)) throw new InvalidConfigException("Column " + joinColumn.name + " already defined");
                columns.add(joinColumn);
            }
        }

        // @JoinTable
        List<Field> joinTableFields = getFieldsWithAnnotation(allFields, JoinTable.class);

        for (Field joinTableField : joinTableFields) {
            List<ColumnObject> joinColumns = ColumnObject.joinTableFieldToColumnObjects(joinTableField);

            for (ColumnObject joinColumn : joinColumns) {
                if (columns.contains(joinColumn)) throw new InvalidConfigException("Column " + joinColumn.name + " already defined");
                columns.add(joinColumn);
            }
        }

        // Parent inherited columns (in case of TABLE_PER_CLASS inheritance strategy)
        if (parent != null && parent.inheritanceType == InheritanceType.TABLE_PER_CLASS) {
            List<ColumnObject> parentColumns = parent.getColumns();

            for (ColumnObject column : parentColumns) {
                if (columns.contains(column)) throw new InvalidConfigException("Column " + column.name + " already defined");
                columns.add(column);
            }
        }

        // Parent inherited primary keys (in case of JOINED inheritance strategy)
        if (parent != null && parent.inheritanceType == InheritanceType.JOINED) {
            List<ColumnObject> parentPrimaryKeys = parent.getPrimaryKeys();

            for (ColumnObject column : parentPrimaryKeys) {
                if (columns.contains(column)) throw new InvalidConfigException("Column " + column.name + " already defined");
                columns.add(column);
            }
        }

        // Children columns (in case of SINGLE_TABLE inheritance strategy)
        if (inheritanceType == InheritanceType.SINGLE_TABLE) {
            for (EntityObject child : children) {
                List<ColumnObject> childColumns = child.getColumns();

                for (ColumnObject column : childColumns) {
                    if (columns.contains(column)) throw new InvalidConfigException("Column " + column.name + " already defined");
                    columns.add(column);
                }
            }
        }

        return columns;
    }


    /**
     * Get primary keys
     *
     * @return  list of primary keys
     */
    List<ColumnObject> getPrimaryKeys() {
        List<ColumnObject> result = new ArrayList<>(getColumns());

        for (Iterator<ColumnObject> it = result.iterator(); it.hasNext();) {
            ColumnObject column = it.next();
            if (!column.primaryKey) it.remove();
        }

        return result;
    }


    /**
     * Get multiple unique columns (specified in the @Table annotation)
     *
     * @return  list of unique columns sets
     */
    List<List<ColumnObject>> getMultipleUniqueColumns() {
        List<List<ColumnObject>> result = new ArrayList<>();

        // Single unique column constraints
        List<ColumnObject> columns = getColumns();

        // Multiple unique columns constraints (their consistence must be checked later, when all the tables have their columns assigned)
        if (modelClass.isAnnotationPresent(Table.class)) {
            Table table = modelClass.getAnnotation(Table.class);
            UniqueConstraint[] uniqueConstraints = table.uniqueConstraints();

            for (UniqueConstraint uniqueConstraint : uniqueConstraints) {
                List<ColumnObject> multipleUniqueColumns = new ArrayList<>(uniqueConstraint.columnNames().length);

                for (String columnName : uniqueConstraint.columnNames()) {
                    ColumnObject column = null;

                    for (ColumnObject co : columns) {
                        if (columnName.equals(co.name)) {
                            column = co;
                            break;
                        }
                    }

                    if (column == null)
                        throw new InvalidConfigException("Unique constraint: column " + columnName + " not found");

                    multipleUniqueColumns.add(column);
                }

                result.add(multipleUniqueColumns);
            }
        }

        // Parent inherited constraints (in case of TABLE_PER_CLASS inheritance strategy)
        if (parent != null && parent.inheritanceType == InheritanceType.TABLE_PER_CLASS) {
            List<List<ColumnObject>> parentConstraints = parent.getMultipleUniqueColumns();
            result.addAll(parentConstraints);
        }

        // Children constraints (in case of SINGLE_TABLE inheritance strategy)
        if (inheritanceType == InheritanceType.SINGLE_TABLE) {
            for (EntityObject child : children) {
                List<List<ColumnObject>> childConstrains = child.getMultipleUniqueColumns();
                result.addAll(childConstrains);
            }
        }

        return result;
    }


    /**
     * Search column by name (search n class, parent and children)
     *
     * @param   name        String      column name to search
     * @return  column object (null if not found)
     */
    @Nullable
    ColumnObject searchColumn(String name) {
        // Entity
        List<ColumnObject> columns = getColumns();

        for (ColumnObject column : columns) {
            if (column.name.equals(name))
                return column;
        }

        // Parent
        if (parent != null) {
            return parent.searchColumn(name);
        }

        // Children
        for (EntityObject child : children) {
            ColumnObject childResult = child.searchColumn(name);
            if (childResult != null) return childResult;
        }

        return null;
    }


    /**
     * Check entity consistence
     *
     * @param   entities    List    list of all entities
     * @throws  KaolDBException if the configuration is invalid
     */
    void checkConsistence(List<EntityObject> entities) {
        // Table name
        if (isRealTable() && tableName == null) {
            throw new InvalidConfigException("Entity " + modelClass.getSimpleName() + " can't have empty table name");
        }

        // A TABLE_PER_CLASS model can't have a SINGLE_TABLE parent
        /*if (inheritanceType == InheritanceType.TABLE_PER_CLASS && parent != null && parent.inheritanceType == InheritanceType.SINGLE_TABLE) {
            throw new KaolDBException(parent.modelClass.getSimpleName() + "is not allowed to have children with an inheritance type different than SINGLE_TABLE");
        }*/

        List<ColumnObject> columns = getColumns();

        for (ColumnObject column : columns) {
            column.checkConsistence(entities);
        }
    }

}
