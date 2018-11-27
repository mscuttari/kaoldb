package it.mscuttari.kaoldb.core;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    /** Database the entity belongs to */
    @NonNull public final DatabaseObject db;

    /** Entity class */
    @NonNull public final Class<?> entityClass;

    /**
     * Table name.
     * Null if the entity doesn't require a real table.
     */
    @Nullable public final String tableName;

    /** Whether the entity has a real table or not */
    public final boolean realTable;

    /** Columns of the table */
    public Columns columns;

    /**
     * Inheritance type.
     * Null if the entity has no children.
     */
    @Nullable public InheritanceType inheritanceType;

    /**
     * Discriminator column.
     * Null until the entities relationships has not been checked with {@link EntityObject#checkConsistence(Map)}.
     */
    public BaseColumnObject discriminatorColumn;

    /**
     * Discriminator value.
     * Null if the entity has no parent.
     */
    @Nullable public Object discriminatorValue;

    /**
     * Parent entity.
     * Null if the entity has no parent.
     */
    @Nullable public EntityObject parent;

    /** Children entities */
    public Collection<EntityObject> children;

    /**
     * Relationships
     *
     * The collection contains the fields declared in {@link #entityClass} (superclasses are
     * excluded) that are annotated with {@link OneToOne}, {@link OneToMany}, {@link ManyToOne}
     * or {@link ManyToMany}.
     */
    public Collection<Field> relationships;


    /**
     * Constructor
     *
     * @param   db              database the entity belongs to
     * @param   entityClass     entity class
     * @param   classes         collection of all classes
     * @param   entitiesMap     map between entity classes and objects
     */
    private EntityObject(DatabaseObject db, Class<?> entityClass, Collection<Class<?>> classes, Map<Class<?>, EntityObject> entitiesMap) {
        this.db = db;
        this.entityClass = entityClass;
        this.tableName = getTableName(entityClass, classes);
        this.realTable = isRealTable(entityClass, classes);
        this.inheritanceType = getInheritanceType(entityClass);
        this.children = new HashSet<>();
        searchParentAndChildren(entitiesMap);
        this.discriminatorValue = entityClass.isAnnotationPresent(DiscriminatorValue.class) ? entityClass.getAnnotation(DiscriminatorValue.class).value() : null;
        this.relationships = getRelationships(entityClass);
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("Class: ")
                .append(getName())
                .append(", ");

        if (this.parent != null) {
            result.append("Parent: ")
                    .append(this.parent.getName())
                    .append(", ");
        }

        result.append("Children: [");

        for (EntityObject child : this.children) {
            result.append(child.getName()).append(", ");
        }

        result.append("], ");

        result.append("Columns: ")
                .append(this.columns)
                .append(", ");

        result.append("Primary keys: ")
                .append(this.columns.getPrimaryKeys());

        return result.toString();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityObject))
            return false;

        EntityObject entityObject = (EntityObject) obj;

        return (entityClass == null && entityObject.entityClass == null) ||
                (entityClass != null && entityClass.equals(entityObject.entityClass));
    }


    @Override
    public int hashCode() {
        return entityClass.hashCode();
    }


    /**
     * Get entity object given entity class
     *
     * @param   db              database the entity belongs to
     * @param   entityClass     model class
     * @param   classes         collection of all classes
     * @param   entitiesMap     map between entity classes and objects
     *
     * @return  table object
     */
    static EntityObject entityClassToEntityObject(DatabaseObject db, Class<?> entityClass, Collection<Class<?>> classes, Map<Class<?>, EntityObject> entitiesMap) {
        return new EntityObject(db, entityClass, classes, entitiesMap);
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
                InheritanceType parentInheritanceType = getInheritanceType(parentClass);
                return parentInheritanceType != InheritanceType.SINGLE_TABLE;
        }

        throw new KaolDBException("Inheritance type not found");
    }


    /**
     * Search and assign parent and children
     *
     * @param   entitiesMap     map between entity classes and objects
     */
    private void searchParentAndChildren(Map<Class<?>, EntityObject> entitiesMap) {
        Collection<Class<?>> classes = db.getEntityClasses();

        // Parent
        Class<?> superClass = entityClass.getSuperclass();
        EntityObject parent = null;

        while (superClass != Object.class && parent == null) {
            parent = entitiesMap.get(superClass);

            if (parent == null && classes.contains(superClass)) {
                parent = entityClassToEntityObject(db, superClass, classes, entitiesMap);
                entitiesMap.put(parent.entityClass, parent);
            }

            superClass = superClass.getSuperclass();
        }

        this.parent = parent;

        // Children
        if (this.parent != null) {
            this.parent.children.add(this);
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
     * By normal columns are intended the one originated from a {@link Column} annotated field.
     *
     * The join columns are derived from:
     *  -   Fields annotated with {@link OneToOne} that are owning side
     *  -   Fields annotated with {@link ManyToOne}
     *
     * @return  collection of all columns
     * @throws  InvalidConfigException in case of multiple column declaration
     */
    private Columns getColumns() {
        Field[] allFields = this.entityClass.getDeclaredFields();
        Columns result = new Columns();

        for (Field field : allFields) {
            if (field.isAnnotationPresent(Column.class)) {
                result.addAll(Columns.entityFieldToColumns(db, this, field));

            } else if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne annotation = field.getAnnotation(OneToOne.class);

                if (!annotation.mappedBy().isEmpty())
                    continue;

                result.addAll(Columns.entityFieldToColumns(db, this, field));

            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                result.addAll(Columns.entityFieldToColumns(db, this, field));
            }

            // Fields annotated with @OneToMany and @ManyToMany are skipped because they don't lead to new columns.
            // In fact, those annotations should only map the existing table columns to the join table ones.
        }

        // Parent inherited primary keys (in case of JOINED inheritance strategy)
        if (this.parent != null && this.parent.inheritanceType == InheritanceType.JOINED) {
            result.addAll(this.parent.getColumns().getPrimaryKeys());
        }

        // Children columns (in case of SINGLE_TABLE inheritance strategy)
        if (this.inheritanceType == InheritanceType.SINGLE_TABLE) {
            for (EntityObject child : children) {
                result.addAll(child.getColumns());
            }
        }

        return result;
    }


    /**
     * Get the unique columns sets (both the unique columns specified in the @Table annotation)
     *
     * @return  list of unique columns sets
     * @see Table#uniqueConstraints()
     */
    private Collection<Collection<BaseColumnObject>> getMultipleUniqueColumns() {
        Collection<Collection<BaseColumnObject>> result = new ArrayList<>();

        // Single unique column constraints
        Columns columns = getColumns();

        // Multiple unique columns constraints (their consistence must be checked later, when all the tables have their columns assigned)
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            UniqueConstraint[] uniqueConstraints = table.uniqueConstraints();

            for (UniqueConstraint uniqueConstraint : uniqueConstraints) {
                List<BaseColumnObject> multipleUniqueColumns = new ArrayList<>(uniqueConstraint.columnNames().length);

                for (String columnName : uniqueConstraint.columnNames()) {
                    BaseColumnObject column = columns.getNamesMap().get(columnName);

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
                Collection<Collection<BaseColumnObject>> childConstrains = child.getMultipleUniqueColumns();
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

        // Discriminator column
        if (this.children.size() != 0) {
            if (!this.entityClass.isAnnotationPresent(Inheritance.class))
                throw new InvalidConfigException("Class " + getName() + " doesn't have @Inheritance annotation");

            if (!this.entityClass.isAnnotationPresent(DiscriminatorColumn.class))
                throw new InvalidConfigException("Class " + getName() + " has @Inheritance annotation but not @DiscriminatorColumn");

            DiscriminatorColumn discriminatorColumnAnnotation = this.entityClass.getAnnotation(DiscriminatorColumn.class);
            if (discriminatorColumnAnnotation.name().isEmpty())
                throw new InvalidConfigException("Class " + getName() + ": empty discriminator column");

            this.discriminatorColumn = columns.getNamesMap().get(discriminatorColumnAnnotation.name());

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
        for (ColumnsContainer column : this.columns) {
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
     * Get field of a class given its name.
     * The field is already set as accessible using {@link Field#setAccessible(boolean)}.
     *
     * @param   fieldName   field name
     * @return  accessible field
     * @throws  MappingException if there is no field in the class with the specified name
     */
    public Field getField(String fieldName) {
        return getField(entityClass, fieldName);
    }


    /**
     * Get field of a class given its name.
     * The field is already set as accessible using {@link Field#setAccessible(boolean)}.
     *
     * @param   clazz       class the field belongs to
     * @param   fieldName   field name
     *
     * @return  accessible field
     *
     * @throws  MappingException if there is no field in the class with the specified name
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            field.setAccessible(true);
            return field;

        } catch (NoSuchFieldException e) {
            throw new MappingException("Field \"" + fieldName + "\" not found in class \"" + clazz.getSimpleName() + "\"", e);
        }
    }


    /**
     * Get the SQL query to create an entity table
     *
     * The result can be used to create just the table directly linked to the provided entity.
     * Optional join tables that are related to eventual internal fields must be managed
     * separately and in a second moment (after the creation of all the normal tables).
     *
     * @return  SQL query (null if no table should be created)
     */
    @Nullable
    public String getSQL() {
        // Skip entity if doesn't require a real table
        if (!realTable)
            return null;

        StringBuilder result = new StringBuilder();

        // Table name
        result.append("CREATE TABLE IF NOT EXISTS ")
                .append(tableName)
                .append(" (");

        // Columns
        String columnSql = columns.getSQL();

        if (columnSql != null && !columnSql.isEmpty())
            result.append(columnSql);

        // Primary keys
        String primaryKeysSql = getTablePrimaryKeysSql(columns.getPrimaryKeys());

        if (primaryKeysSql != null && !primaryKeysSql.isEmpty()) {
            result.append(", ").append(primaryKeysSql);
        }

        // Unique keys (multiple columns)
        String uniqueKeysSql = getTableUniquesSql(getMultipleUniqueColumns());

        if (uniqueKeysSql != null && !uniqueKeysSql.isEmpty()) {
            result.append(", ").append(uniqueKeysSql);
        }

        // Foreign keys
        String foreignKeysSql = getTableForeignKeysSql();

        if (foreignKeysSql != null && !foreignKeysSql.isEmpty()) {
            result.append(", ").append(foreignKeysSql);
        }

        result.append(");");
        //result.append(") WITHOUT ROWID;");

        return result.toString();
    }


    /**
     * Get primary keys SQL statement to be inserted in the create table query
     *
     * This method is used to create the primary keys of a table directly associated to an entity.
     * There is no counterpart for the join tables because that can be directly managed by this
     * method.
     *
     * Example: PRIMARY KEY(column_1, column_2, column_3)
     *
     * @param   primaryKeys     collection of primary keys
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    public static String getTablePrimaryKeysSql(Collection<BaseColumnObject> primaryKeys) {
        if (primaryKeys == null || primaryKeys.size() == 0)
            return null;

        StringBuilder result = new StringBuilder();
        String prefix = "PRIMARY KEY(";

        for (BaseColumnObject column : primaryKeys) {
            result.append(prefix).append(column.name);
            prefix = ", ";
        }

        result.append(")");
        return result.toString();
    }



    /**
     * Get unique columns SQL statement to be inserted in the create table query
     *
     * This method is used to create the unique constraints defined using the
     * {@link UniqueConstraint} annotation.
     * There is no counterpart for join tables because the unique constraints of a join table
     * are given just by the primary keys.
     *
     * The parameter uniqueColumns is a collection of collections because each unique constraint
     * can be made of multiple columns. For example, a collection such as
     * [[column_1, column_2], [column_2, column_3, column_4]] would generate the statement
     * UNIQUE(column_1, column_2), UNIQUE(column_2, column_3, column_4)
     *
     * @param   uniqueColumns       unique columns groups
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private static String getTableUniquesSql(Collection<Collection<BaseColumnObject>> uniqueColumns) {
        if (uniqueColumns == null || uniqueColumns.size() == 0)
            return null;

        StringBuilder result = new StringBuilder();
        boolean empty = true;

        for (Collection<BaseColumnObject> uniqueSet : uniqueColumns) {
            if (uniqueSet.size() == 0)
                continue;

            if (!empty) result.append(", ");
            empty = false;
            result.append("UNIQUE(");

            String prefixInternal = "UNIQUE(";

            for (BaseColumnObject column : uniqueSet) {
                result.append(prefixInternal).append(column.name);
                prefixInternal = ", ";
            }

            result.append(")");
        }

        if (empty) {
            return null;
        } else {
            return result.toString();
        }
    }




    /**
     * Get the foreign keys SQL constraints to be inserted in the create table query
     *
     * This method is used to create the foreign keys of a table directly associated to an entity.
     * See {@link } to do the same for a join table.
     *
     * Example:
     * FOREIGN KEY (column_1, column_2) REFERENCES referenced_table_1(referenced_column_1, referenced_column_2),
     * FOREIGN KEY (column_3, column_4) REFERENCES referenced_table_2(referenced_column_3, referenced_column_4),
     * FOREIGN KEY (column_5, column_6) REFERENCES referenced_table_3(referenced_column_5, referenced_column_6)
     *
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getTableForeignKeysSql() {
        Collection<String> constraints = new ArrayList<>();

        // Inheritance
        String inheritanceSql = getTableInheritanceConstraints();

        if (inheritanceSql != null && !inheritanceSql.isEmpty())
            constraints.add(inheritanceSql);

        // Relationships
        String relationshipsSql = getTableRelationshipsConstraints();

        if (relationshipsSql != null && !relationshipsSql.isEmpty())
            constraints.add(relationshipsSql);

        // Create SQL statement
        if (constraints.size() == 0)
            return null;

        return TextUtils.join(", ", constraints);
    }


    /**
     * Get the inheritance SQL constraint to be inserted in the create table query
     *
     * Example: FOREIGN KEY (primary_key_1, primary_key_2)
     *          REFERENCES parent_table(primary_key_1, primary_key_2)
     *          ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
     *
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getTableInheritanceConstraints() {
        if (this.parent == null)
            return null;

        EntityObject parent = this.parent;

        // Go up in hierarchy until there is a real table
        while (parent != null && !parent.realTable)
            parent = parent.parent;

        // Check if there's a real parent table (TABLE_PER_CLASS strategy makes this unknown)
        if (parent == null)
            return null;

        Collection<BaseColumnObject> parentPrimaryKeys = parent.columns.getPrimaryKeys();

        // Normally not happening
        if (parentPrimaryKeys.size() == 0)
            return null;

        // Create associations
        StringBuilder local = new StringBuilder();          // Local columns
        StringBuilder referenced = new StringBuilder();     // Referenced columns

        String separator = "";

        for (BaseColumnObject primaryKey : parentPrimaryKeys) {
            local.append(separator).append(primaryKey.name);
            referenced.append(separator).append(primaryKey.name);

            separator = ", ";
        }

        return "FOREIGN KEY(" + local.toString() + ")" +
                " REFERENCES " + parent.tableName + "(" + referenced.toString() + ")" +
                " ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED";
    }


    /**
     * Get the relationships SQL constraints to be inserted in the create table query
     *
     * This method is used to create the foreign keys of a table directly associated to an entity.
     * There is no counterpart of this method for join tables because their foreign keys are
     * completely covered by {@link }.
     *
     * Example:
     *  FOREIGN KEY (column_1, column_2)
     *  REFERENCES referenced_table_1(referenced_column_1, referenced_column_2)
     *  ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
     *
     *  FOREIGN KEY (column_3, column_4)
     *  REFERENCES referenced_table_2(referenced_column_3, referenced_column_4)
     *  ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
     *
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getTableRelationshipsConstraints() {
        boolean empty = true;
        StringBuilder result = new StringBuilder();

        for (Field field : relationships) {
            // Join columns are present only if there is a @JoinColumn annotation or if
            // there is a @JoinColumns annotation and its content is not empty.

            if (!field.isAnnotationPresent(JoinColumn.class) &&
                    ((!field.isAnnotationPresent(JoinColumns.class) || (field.isAnnotationPresent(JoinColumns.class) && field.getAnnotation(JoinColumns.class).value().length == 0))))
                continue;

            if (!empty) result.append(", ");
            empty = false;

            // Get the linked entity in order to get its table name
            EntityObject linkedEntity = db.getEntity(field.getType());

            // @JoinColumn
            if (field.isAnnotationPresent(JoinColumn.class)) {
                JoinColumn annotation = field.getAnnotation(JoinColumn.class);

                result.append("FOREIGN KEY(").append(annotation.name()).append(")")
                        .append(" REFERENCES ").append(linkedEntity.tableName).append("(")
                        .append(annotation.referencedColumnName()).append(")")
                        .append(" ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED");
            }

            // @JoinColumns
            if (field.isAnnotationPresent(JoinColumns.class)) {
                JoinColumns annotation = field.getAnnotation(JoinColumns.class);

                StringBuilder local = new StringBuilder();          // Local columns
                StringBuilder referenced = new StringBuilder();     // Referenced columns

                String separator = "";

                for (JoinColumn joinColumn : annotation.value()) {
                    local.append(separator).append(joinColumn.name());
                    referenced.append(separator).append(joinColumn.referencedColumnName());

                    separator = ", ";
                }

                result.append("FOREIGN KEY(").append(local).append(")")
                        .append(" REFERENCES ").append(linkedEntity.tableName).append("(").append(referenced).append(")")
                        .append(" ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED");
            }
        }

        if (empty) {
            return null;
        } else {
            return result.toString();
        }
    }

}
