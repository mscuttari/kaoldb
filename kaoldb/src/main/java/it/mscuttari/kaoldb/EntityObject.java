package it.mscuttari.kaoldb;

import android.content.ContentValues;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
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

public class EntityObject {

    // Entity class
    public Class<?> entityClass;

    // Table name
    @Nullable
    public String tableName;

    // Real table
    public boolean realTable;

    // Columns
    public Collection<ColumnObject> columns;            // All columns
    public Map<String, ColumnObject> columnsNameMap;    // Mapped by column name (all columns)
    public Map<String, ColumnObject> columnsFieldMap;   // Mapped by field name (only current entity columns)

    // Primary keys
    public Collection<ColumnObject> primaryKeys;

    // Inheritance type
    @Nullable
    public InheritanceType inheritanceType;

    // Discriminator column
    // This is null until the entities relationships has not been checked
    public ColumnObject discriminatorColumn;

    // Discriminator value
    public Object discriminatorValue;

    // Parent
    @Nullable
    public EntityObject parent;

    // Children
    public Collection<EntityObject> children;


    /**
     * Constructor
     *
     * @param   entityClass     entity class
     * @param   classes         collection of all classes
     * @param   entitesMap      map between entity classes and objects
     */
    private EntityObject(Class<?> entityClass, Collection<Class<?>> classes, Map<Class<?>, EntityObject> entitesMap) {
        this.entityClass = entityClass;
        this.tableName = getTableName(entityClass, classes);
        this.realTable = isRealTable(entityClass, classes);
        this.columns = null;
        this.columnsNameMap = null;
        this.columnsFieldMap = null;
        this.inheritanceType = getInheritanceType(entityClass);
        this.parent = null;
        this.children = new HashSet<>();
        searchParentAndChildren(this, classes, entitesMap);
        this.discriminatorColumn  = null;
        this.discriminatorValue = entityClass.isAnnotationPresent(DiscriminatorValue.class) ? entityClass.getAnnotation(DiscriminatorValue.class).value() : null;
    }


    @Override
    public String toString() {
        String result = "";

        result += "Class: " + this.entityClass.getSimpleName() + ", ";

        if (this.parent != null)
            result += "Parent: " + this.parent.entityClass.getSimpleName() + ", ";

        result += "Children: [";
        for (EntityObject child : this.children) {
            //noinspection StringConcatenationInLoop
            result += child.entityClass.getSimpleName() + ", ";
        }
        result += "], ";

        result += "Columns: " + this.columns + ", ";
        result += "Primary keys: " + this.primaryKeys;

        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof EntityObject)) return false;
        EntityObject columnObject = (EntityObject)obj;
        if (entityClass == null && columnObject.entityClass == null) return true;
        if (entityClass != null) return entityClass.equals(columnObject.entityClass);
        return false;
    }


    @Override
    public int hashCode() {
        return this.entityClass.getSimpleName().hashCode();
    }


    /**
     * Get entity object given entity class
     *
     * @param   entityClass     model class
     * @param   classes         collection of all classes
     * @param   entitiesMap     map between entity classes and objects
     *
     * @return  table object
     */
    static EntityObject entityClassToTableObject(Class<?> entityClass, Collection<Class<?>> classes, Map<Class<?>, EntityObject> entitiesMap) {
        return new EntityObject(entityClass, classes, entitiesMap);
    }


    /**
     * Get table name
     *
     * If the table is not specified, the following policy is applied:
     * Uppercase characters are replaces with underscore followed by the same character converted to lowercase
     * Only the first class tableName character, if uppercase, is converted to lowercase avoiding the underscore
     * Example: ModelClassName => model_class_name
     *
     * @param   entityClass     entity class
     * @param   classes         collection of all classes
     *
     * @return  table tableName (null if the class doesn't need a real table)
     *
     * @throws  InvalidConfigException if the class doesn't have the @Table annotation and the inheritance type is not TABLE_PER_CLASS
     */
    private static String getTableName(Class<?> entityClass, Collection<Class<?>> classes) {
        if (!entityClass.isAnnotationPresent(Table.class)) {
            if (isRealTable(entityClass, classes)) {
                throw new InvalidConfigException("Class " + entityClass.getSimpleName() + " doesn't have the @Table annotation");
            } else {
                return null;
            }
        }

        // Get specified table tableName
        Table table = entityClass.getAnnotation(Table.class);
        if (!table.name().isEmpty()) return table.name();
        Log.i(LOG_TAG, entityClass.getSimpleName() + ": table tableName not specified, using the default one based on class tableName");

        // Default table tableName
        String className = entityClass.getSimpleName();
        char c[] = className.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        className = new String(c);
        return className.replaceAll("([A-Z])", "_$1").toLowerCase();
    }


    /**
     * Get inheritance type
     *
     * @param   entityClass     entity class
     * @return  inheritance type
     */
    @Nullable
    private static InheritanceType getInheritanceType(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Inheritance.class))
            return null;

        Inheritance inheritance = entityClass.getAnnotation(Inheritance.class);
        return inheritance.strategy();
    }


    /**
     * Determine if the class is linked to a real table
     *
     * @param   entityClass     model class
     * @param   classes         collection of all classes
     *
     * @return  true if a table should exists for this class; false if not
     *
     * @throws  KaolDBException if a newly implemented inheritance type is not taken into consideration
     */
    private static boolean isRealTable(Class<?> entityClass, Collection<Class<?>> classes) {
        InheritanceType inheritanceType = getInheritanceType(entityClass);

        if (inheritanceType == null) {
            Class<?> superClass = entityClass.getSuperclass();

            while (superClass != Object.class) {
                if (classes.contains(superClass))
                    return getInheritanceType(superClass) != InheritanceType.SINGLE_TABLE;

                superClass = superClass.getSuperclass();
            }

            return true;
        }

        switch (inheritanceType) {
            case SINGLE_TABLE:
            case JOINED:
                Class<?> parentClass = entityClass.getSuperclass();
                InheritanceType parentInheritancetype = getInheritanceType(parentClass);
                return parentInheritancetype != InheritanceType.SINGLE_TABLE;
        }

        throw new KaolDBException("Inheritance type not found");
    }


    /**
     * Get the fields with a specific annotation
     *
     * @param   fields              fields array to search in
     * @param   annotationClass     desired annotation class
     *
     * @return  collection of fields with the annotation specified
     */
    private static Collection<Field> getFieldsWithAnnotation(Field[] fields, Class<? extends Annotation> annotationClass) {
        Collection<Field> result = new ArrayList<>();

        for (Field field : fields) {
            if (field.isAnnotationPresent(annotationClass))
                result.add(field);
        }

        return result;
    }


    /**
     * Search and assign parent and children
     *
     * @param   entity          entity to search for parent and children
     * @param   classes         collection of all classes
     * @param   entitiesMap     map between entity classes and objects
     */
    private static void searchParentAndChildren(EntityObject entity, Collection<Class<?>> classes, Map<Class<?>, EntityObject> entitiesMap) {
        // Parent
        Class<?> superClass = entity.entityClass.getSuperclass();
        EntityObject parent = null;

        while (superClass != Object.class && parent == null) {
            parent = entitiesMap.get(superClass);

            if (parent == null && classes.contains(superClass)) {
                parent = entityClassToTableObject(superClass, classes, entitiesMap);
                entitiesMap.put(parent.entityClass, parent);
            }

            superClass = superClass.getSuperclass();
        }

        entity.parent = parent;

        // Children
        if (entity.parent != null) {
            entity.parent.children.add(entity);
        }
    }


    /**
     * Load columns
     *
     * @return  collection of all columns
     * @throws  InvalidConfigException in case of multiple column declaration
     */
    private Collection<ColumnObject> getColumns() {
        Field[] allFields = this.entityClass.getDeclaredFields();

        // Normal columns
        Collection<ColumnObject> normalColumns = getNormalColumns(allFields);
        Collection<ColumnObject> result = new HashSet<>(normalColumns);

        // Join columns
        Collection<ColumnObject> joinColumns = getJoinColumns(allFields);

        if (!Collections.disjoint(result, joinColumns))
            throw new InvalidConfigException("Class " + this.entityClass.getSimpleName() + ": some columns are defined more than once");

        result.addAll(joinColumns);

        // Parent inherited primary keys (in case of JOINED inheritance strategy)
        if (this.parent != null && this.parent.inheritanceType == InheritanceType.JOINED) {
            Collection<ColumnObject> parentPrimaryKeys = this.parent.getPrimaryKeys();

            if (!Collections.disjoint(result, parentPrimaryKeys))
                throw new InvalidConfigException("Class " + this.entityClass.getSimpleName() + ": some columns clash with inherited primary keys");

            result.addAll(parentPrimaryKeys);
        }

        // Children columns (in case of SINGLE_TABLE inheritance strategy)
        if (this.inheritanceType == InheritanceType.SINGLE_TABLE) {
            for (EntityObject child : children) {
                Collection<ColumnObject> childColumns = child.getColumns();

                if (!Collections.disjoint(result, childColumns))
                    throw new InvalidConfigException("Class " + this.entityClass.getSimpleName() + ": some columns clash with included columns from " + child.entityClass.getSimpleName());

                result.addAll(childColumns);
            }
        }

        return result;
    }


    /**
     * Get normal columns (only of the current class)
     *
     * @param   allFields       collection of all class fields
     * @return  collection of columns
     */
    private static Collection<ColumnObject> getNormalColumns(Field[] allFields) {
        Collection<Field> fields = getFieldsWithAnnotation(allFields, Column.class);
        Collection<ColumnObject> result = new HashSet<>(fields.size());

        for (Field field : fields) {
            ColumnObject column = ColumnObject.columnFieldToColumnObject(field);
            if (result.contains(column)) throw new InvalidConfigException("Column " + column.name + " already defined");
            result.add(column);
        }

        return result;
    }


    /**
     * Get From columns (only of the current class)
     *
     * @param   allFields       collection of all class fields
     * @return  collection of From columns
     */
    private static Collection<ColumnObject> getJoinColumns(Field[] allFields) {
        Collection<ColumnObject> result = new HashSet<>();

        // @JoinColumn
        Collection<Field> joinColumnFields = getFieldsWithAnnotation(allFields, JoinColumn.class);

        for (Field field : joinColumnFields) {
            ColumnObject column = ColumnObject.columnFieldToColumnObject(field);
            if (result.contains(column)) throw new InvalidConfigException("Column " + column.name + " already defined");
            result.add(column);
        }

        // @JoinColumns
        Collection<Field> joinColumnsFields = getFieldsWithAnnotation(allFields, JoinColumns.class);

        for (Field field : joinColumnsFields) {
            List<ColumnObject> joinColumns = ColumnObject.joinColumnsFieldToColumnObjects(field);

            for (ColumnObject column : joinColumns) {
                if (result.contains(column)) throw new InvalidConfigException("Column " + column.name + " already defined");
                result.add(column);
            }
        }

        // @JoinTable
        Collection<Field> joinTableFields = getFieldsWithAnnotation(allFields, JoinTable.class);

        for (Field field : joinTableFields) {
            List<ColumnObject> columns = ColumnObject.joinTableFieldToColumnObjects(field);

            for (ColumnObject column : columns) {
                if (result.contains(column)) throw new InvalidConfigException("Column " + column.name + " already defined");
                result.add(column);
            }
        }

        return result;
    }


    /**
     * Get primary keys
     *
     * @return  collection of primary keys
     */
    private Collection<ColumnObject> getPrimaryKeys() {
        Collection<ColumnObject> result = new HashSet<>();
        Collection<ColumnObject> columns = getColumns();

        for (ColumnObject column : columns) {
            if (column.primaryKey)
                result.add(column);
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
        Collection<ColumnObject> columns = getColumns();

        // Multiple unique columns constraints (their consistence must be checked later, when all the tables have their columns assigned)
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
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
     * Setup entity columns
     */
    void setupColumns() {
        // Columns
        this.columns = getColumns();

        // Map between field name and column object
        Collection<ColumnObject> classNormalColumns = getNormalColumns(this.entityClass.getDeclaredFields());
        columnsFieldMap = new HashMap<>();

        for (ColumnObject column : classNormalColumns) {
            columnsFieldMap.put(column.field.getName(), column);
        }

        // Map between column name and column object
        columnsNameMap = new HashMap<>(this.columns.size());

        for (ColumnObject column : this.columns) {
            columnsNameMap.put(column.name, column);
        }

        // Primary keys
        this.primaryKeys = getPrimaryKeys();

        // Discriminator column
        if (this.children.size() != 0) {
            if (!this.entityClass.isAnnotationPresent(Inheritance.class))
                throw new InvalidConfigException("Class " + this.entityClass.getSimpleName() + " doesn't have @Inheritance annotation");

            if (!this.entityClass.isAnnotationPresent(DiscriminatorColumn.class))
                throw new InvalidConfigException("Class " + this.entityClass.getSimpleName() + " has @Inheritance annotation but not @DiscriminatorColumn");

            DiscriminatorColumn discriminatorColumnAnnotation = this.entityClass.getAnnotation(DiscriminatorColumn.class);
            if (discriminatorColumnAnnotation.name().isEmpty())
                throw new InvalidConfigException("Class " + this.entityClass.getSimpleName() + ": empty discriminator column");

            this.discriminatorColumn = this.columnsNameMap.get(discriminatorColumnAnnotation.name());

            if (discriminatorColumn == null)
                throw new InvalidConfigException("Class " + this.entityClass.getSimpleName() + ": discriminator column " + discriminatorColumnAnnotation.name() + " not found");

            discriminatorColumn.nullable = false;
        }

        // Discriminator value
        if (this.parent != null && discriminatorValue == null) {
            throw new InvalidConfigException("Class " + this.entityClass.getSimpleName() + " doesn't have @DiscriminatorValue annotation");
        }
    }


    /**
     * Check entity consistence
     *
     * @param   entities        map of all entities
     * @throws  KaolDBException if the configuration is invalid
     */
    void checkConsistence(Map<Class<?>, EntityObject> entities) {
        // Table name
        if (this.realTable && this.tableName == null)
            throw new InvalidConfigException("Entity " + this.entityClass.getSimpleName() + " can't have empty table name");

        // Join columns consistence
        for (ColumnObject column : this.columns) {
            column.checkConsistence(entities);
        }

        // Fix discriminator value type
        if (this.discriminatorColumn != null) {
            DiscriminatorColumn discriminatorColumnAnnotation = this.entityClass.getAnnotation(DiscriminatorColumn.class);

            for (EntityObject child : this.children) {
                switch (discriminatorColumnAnnotation.discriminatorType()) {
                    case INTEGER:
                        child.discriminatorValue = Integer.valueOf((String)child.discriminatorValue);
                        break;
                }
            }
        }
    }

}
