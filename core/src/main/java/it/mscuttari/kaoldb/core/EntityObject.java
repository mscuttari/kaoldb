package it.mscuttari.kaoldb.core;

import android.support.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Inheritance;
import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.annotations.Table;
import it.mscuttari.kaoldb.annotations.UniqueConstraint;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.exceptions.KaolDBException;
import it.mscuttari.kaoldb.exceptions.MappingException;

/**
 * Each {@link EntityObject} maps a class annotated with the {@link Entity} annotation.
 * Mapping includes table columns, parent and children classes.
 */
class EntityObject {

    /** Entity class */
    public Class<?> entityClass;

    /**
     * Table name.
     * Null if the entity doesn't require a real table.
     */
    @Nullable
    public String tableName;

    /** Whether the entity has a real table or not */
    public boolean realTable;

    /** Columns */
    public Collection<ColumnObject> columns;            // All columns
    public Map<String, ColumnObject> columnsNameMap;    // All columns mapped by column name

    /** Primary keys */
    public Collection<ColumnObject> primaryKeys;

    /**
     * Inheritance type.
     * Null if the entity has no children.
     */
    @Nullable
    public InheritanceType inheritanceType;

    /**
     * Discriminator column name.
     * Null until the entities relationships has not been checked with {@link EntityObject#checkConsistence(Map)}.
     */
    public ColumnObject discriminatorColumn;

    /**
     * Discriminator value.
     * Null if the entity has no parent.
     */
    @Nullable
    public Object discriminatorValue;

    /**
     * Parent entity.
     * Null if the entity has no parent.
     */
    @Nullable
    public EntityObject parent;

    /** Children entities */
    public Collection<EntityObject> children;

    /** Relationships */
    public Collection<Field> relationships;


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
        this.inheritanceType = getInheritanceType(entityClass);
        this.children = new HashSet<>();
        searchParentAndChildren(this, classes, entitesMap);
        this.discriminatorValue = entityClass.isAnnotationPresent(DiscriminatorValue.class) ? entityClass.getAnnotation(DiscriminatorValue.class).value() : null;
        this.relationships = getRelationships(entityClass);
    }


    @Override
    public String toString() {
        String result = "";

        result += "Class: " + getName() + ", ";

        if (this.parent != null)
            result += "Parent: " + this.parent.getName() + ", ";

        result += "Children: [";
        for (EntityObject child : this.children) {
            //noinspection StringConcatenationInLoop
            result += child.getName() + ", ";
        }
        result += "], ";

        result += "Columns: " + this.columns + ", ";
        result += "Primary keys: " + this.primaryKeys;

        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof EntityObject)) return false;
        EntityObject entityObject = (EntityObject)obj;
        if (entityClass == null && entityObject.entityClass == null) return true;
        if (entityClass != null) return entityClass.equals(entityObject.entityClass);
        return false;
    }


    @Override
    public int hashCode() {
        return getName().hashCode();
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
    static EntityObject entityClassToEntityObject(Class<?> entityClass, Collection<Class<?>> classes, Map<Class<?>, EntityObject> entitiesMap) {
        return new EntityObject(entityClass, classes, entitiesMap);
    }


    /**
     * Get the simple name of the class associated to this entity
     *
     * @return  class name
     */
    public String getName() {
        return entityClass.getSimpleName();
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
        LogUtils.w(entityClass.getSimpleName() + ": table tableName not specified, using the default one based on class tableName");

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
                parent = entityClassToEntityObject(superClass, classes, entitiesMap);
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
     * Get entity relationships fields
     *
     * Each field is considered a relationship one if it is annotated with {@link OneToOne},
     * {@link OneToMany}, {@link ManyToMany} or {@link ManyToMany}
     *
     * @param   entityClass     entity class
     * @return  relationships fields
     */
    private static Collection<Field> getRelationships(Class<?> entityClass) {
        Collection<Field> relationships = new ArrayList<>();

        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(OneToOne.class) ||
                    field.isAnnotationPresent(OneToMany.class) ||
                    field.isAnnotationPresent(ManyToOne.class) ||
                    field.isAnnotationPresent(ManyToMany.class)) {

                relationships.add(field);
            }
        }

        return Collections.unmodifiableCollection(relationships);
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
            throw new InvalidConfigException("Class " + getName() + ": some columns are defined more than once");

        result.addAll(joinColumns);

        // Parent inherited primary keys (in case of JOINED inheritance strategy)
        if (this.parent != null && this.parent.inheritanceType == InheritanceType.JOINED) {
            Collection<ColumnObject> parentPrimaryKeys = this.parent.getPrimaryKeys();
            checkColumnUniqueness(result, parentPrimaryKeys);
            result.addAll(parentPrimaryKeys);
        }

        // Children columns (in case of SINGLE_TABLE inheritance strategy)
        if (this.inheritanceType == InheritanceType.SINGLE_TABLE) {
            for (EntityObject child : children) {
                Collection<ColumnObject> childColumns = child.getColumns();
                checkColumnUniqueness(result, childColumns);
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
    private Collection<ColumnObject> getNormalColumns(Field[] allFields) {
        Collection<Field> fields = getFieldsWithAnnotation(allFields, Column.class);
        Collection<ColumnObject> result = new HashSet<>(fields.size());

        for (Field field : fields) {
            Collection<ColumnObject> columns = ColumnObject.fieldToObject(field);
            checkColumnUniqueness(result, columns);
            result.addAll(columns);
        }

        return result;
    }


    /**
     * Get From columns (only of the current class)
     *
     * @param   allFields       collection of all class fields
     * @return  collection of From columns
     */
    private Collection<ColumnObject> getJoinColumns(Field[] allFields) {
        Collection<ColumnObject> result = new HashSet<>();

        // @JoinColumn
        Collection<Field> joinColumnFields = getFieldsWithAnnotation(allFields, JoinColumn.class);

        for (Field field : joinColumnFields) {
            Collection<ColumnObject> columns = ColumnObject.fieldToObject(field);
            checkColumnUniqueness(result, columns);
            result.addAll(columns);
        }

        // @JoinColumns
        Collection<Field> joinColumnsFields = getFieldsWithAnnotation(allFields, JoinColumns.class);

        for (Field field : joinColumnsFields) {
            Collection<ColumnObject> columns = ColumnObject.fieldToObject(field);
            checkColumnUniqueness(result, columns);
            result.addAll(columns);
        }

        // Fields annotated with @JoinTable are skipped because they don't lead to new columns.
        // In fact, the annotation should only map the existing table columns to the join table ones.

        return result;
    }


    /**
     * Ensures that all the given columns are not already defined.
     * Used during the columns mapping phase
     *
     * @see #getNormalColumns(Field[])
     * @see #getJoinColumns(Field[])
     *
     * @param   startingColumns     collection where the columns have to be searched
     * @param   columnsToBeAdded    collection of the columns to be added
     *
     * @throws InvalidConfigException if any column has already been defined
     */
    private void checkColumnUniqueness(Collection<ColumnObject> startingColumns, Collection<ColumnObject> columnsToBeAdded) {
        if (Collections.disjoint(startingColumns, columnsToBeAdded))
            return;

        for (ColumnObject column : columnsToBeAdded) {
            if (startingColumns.contains(column))
                throw new InvalidConfigException("Column " + column.name + " already defined");
        }
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

        // Map between column name and column object
        Map<String, ColumnObject> columnsNameMap = new HashMap<>(this.columns.size());
        this.columnsNameMap = Collections.unmodifiableMap(columnsNameMap);

        for (ColumnObject column : this.columns) {
            columnsNameMap.put(column.name, column);
        }

        // Primary keys
        this.primaryKeys = getPrimaryKeys();

        // Discriminator column
        if (this.children.size() != 0) {
            if (!this.entityClass.isAnnotationPresent(Inheritance.class))
                throw new InvalidConfigException("Class " + getName() + " doesn't have @Inheritance annotation");

            if (!this.entityClass.isAnnotationPresent(DiscriminatorColumn.class))
                throw new InvalidConfigException("Class " + getName() + " has @Inheritance annotation but not @DiscriminatorColumn");

            DiscriminatorColumn discriminatorColumnAnnotation = this.entityClass.getAnnotation(DiscriminatorColumn.class);
            if (discriminatorColumnAnnotation.name().isEmpty())
                throw new InvalidConfigException("Class " + getName() + ": empty discriminator column");

            this.discriminatorColumn = columnsNameMap.get(discriminatorColumnAnnotation.name());

            if (discriminatorColumn == null)
                throw new InvalidConfigException("Class " + getName() + ": discriminator column " + discriminatorColumnAnnotation.name() + " not found");

            discriminatorColumn.nullable = false;
        }

        // Discriminator value
        if (this.parent != null && discriminatorValue == null) {
            throw new InvalidConfigException("Class " + getName() + " doesn't have @DiscriminatorValue annotation");
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
            throw new InvalidConfigException("Entity " + getName() + " can't have empty table name");

        // Fix join columns types
        for (ColumnObject column : this.columns) {
            column.fixType(entities);
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


    /**
     * Get field of a class given its name
     *
     * @param   fieldName   field name
     * @return  field
     * @throws  MappingException if there is no field in the class with the specified name
     */
    public Field getField(String fieldName) {
        return getField(entityClass, fieldName);
    }


    /**
     * Get field of a class given its name
     *
     * @param   clazz       class the field belongs to
     * @param   fieldName   field name
     *
     * @return  field
     *
     * @throws  MappingException if there is no field in the class with the specified name
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new MappingException("Field \"" + fieldName + "\" not found in class \"" + clazz.getSimpleName() + "\"", e);
        }
    }


    /**
     * Get all the primary keys, including the parent entities ones
     *
     * @return  primary keys
     */
    public Collection<ColumnObject> getAllPrimaryKeys() {
        Collection<ColumnObject> result = new HashSet<>();
        EntityObject entity = this;

        // Navigate up in the hierarchy tree
        while (entity != null) {
            result.addAll(entity.primaryKeys);
            entity = entity.parent;
        }

        return Collections.unmodifiableCollection(result);
    }

}
