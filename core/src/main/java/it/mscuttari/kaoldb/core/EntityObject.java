package it.mscuttari.kaoldb.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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
import it.mscuttari.kaoldb.exceptions.MappingException;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.mscuttari.kaoldb.core.ConcurrencyUtils.doAndNotifyAll;
import static it.mscuttari.kaoldb.core.ConcurrencyUtils.waitWhile;

/**
 * Each {@link EntityObject} maps a class annotated with the {@link Entity} annotation.
 * Mapping includes table columns, parent and children classes.
 */
class EntityObject<T> {

    /** Database the entity belongs to */
    public final DatabaseObject db;

    /** Entity class */
    public final Class<T> clazz;

    /**
     * Inheritance type.
     * Null if the entity has no children.
     */
    @Nullable
    public InheritanceType inheritanceType;


    /**
     * Parent entity.
     * Null if the entity has no parent.
     */
    @Nullable
    public EntityObject<? super T> parent;

    /**
     * Discriminator value.
     * Null if the entity has no parent.
     */
    @Nullable
    public Object discriminatorValue;

    /** Children entities */
    @NonNull
    public Collection<EntityObject<? extends T>> children = new HashSet<>();

    /** Whether the entity has a real table or not */
    public Boolean realTable;

    /**
     * Table name.
     * Null if the entity doesn't require a real table.
     */
    @Nullable
    public String tableName;

    /** Columns of the table */
    public Columns columns = new Columns(this);

    /** Discriminator column */
    @Nullable
    public BaseColumnObject discriminatorColumn;

    /**
     * Relationships
     *
     * The collection contains the fields declared in {@link #clazz} (superclasses are
     * excluded) that are annotated with {@link OneToOne}, {@link OneToMany}, {@link ManyToOne}
     * or {@link ManyToMany}.
     */
    public Collection<Field> relationships = new HashSet<>();


    /**
     * Constructor
     *
     * @param db        database the entity belongs to
     * @param clazz     entity class
     */
    private EntityObject(@NonNull DatabaseObject db, @NonNull Class<T> clazz) {
        this.db = db;
        this.clazz = checkNotNull(clazz);
    }


    /**
     * Create the entity object linked to an entity class and start the mapping process.
     *
     * @param db        database the entity belongs to
     * @param clazz     entity class
     * @param <T>       entity class
     *
     * @return entity object
     */
    public static <T> EntityObject<T> map(@NonNull DatabaseObject db, @NonNull Class<T> clazz) {
        EntityObject<T> result = new EntityObject<>(db, clazz);

        try {
            return result;

        } finally {
            result.loadInheritanceType();
            result.loadParent();
            result.loadDiscriminatorValue();
            result.loadTableExistence();
            result.loadTableName();
            result.loadRelationships();
        }
    }


    @NonNull
    @Override
    public String toString() {
        return "Class: " + getName() + ", " +
                "Parent: " + (parent == null ? "null" : parent.getName()) + ", " +
                "Children: {" + StringUtils.implode(children, EntityObject::getName, ", ") + "}, " +
                "Columns: {" + columns + "}, " +
                "Primary keys: {" + columns.getPrimaryKeys() + "}";
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityObject))
            return false;

        EntityObject entityObject = (EntityObject) obj;
        return clazz.equals(entityObject.clazz);
    }


    @Override
    public int hashCode() {
        return clazz.hashCode();
    }


    /**
     * Get the simple name of the class associated to this entity
     *
     * @return class name
     */
    public String getName() {
        return clazz.getSimpleName();
    }


    /**
     * Determine the inheritance type
     */
    private void loadInheritanceType() {
        Inheritance annotation = clazz.getAnnotation(Inheritance.class);
        InheritanceType type = annotation == null ? null : annotation.strategy();
        doAndNotifyAll(this, () -> inheritanceType = type);
    }


    /**
     * Determine if the class is linked to a real table.
     * Must be called after {@link #loadParent()}.
     */
    private void loadTableExistence() {
        boolean result;

        if (parent == null) {
            result = true;

        } else {
            waitWhile(parent, () -> parent.inheritanceType == null);
            result = parent.inheritanceType != InheritanceType.SINGLE_TABLE;
        }

        doAndNotifyAll(this, () -> realTable = result);
    }


    /**
     * Determine the table name
     *
     * If the table is not specified, the following policy is applied:
     * Uppercase characters are replaces with underscore followed by the same character converted to lowercase
     * Only the first class tableName character, if uppercase, is converted to lowercase avoiding the underscore
     * Example: ModelClassName => model_class_name
     *
     * @throws InvalidConfigException if the class doesn't have the @Table annotation and the inheritance type is not TABLE_PER_CLASS
     */
    private void loadTableName() {
        String result;
        Table tableAnnotation = clazz.getAnnotation(Table.class);

        if (tableAnnotation == null) {
            waitWhile(this, () -> realTable == null);

            if (realTable) {
                throw new InvalidConfigException("Class " + clazz.getSimpleName() + " doesn't have the @Table annotation");
            } else {
                result = null;
            }

        } else if (!tableAnnotation.name().isEmpty()) {
            // Get specified table name
            result = tableAnnotation.name();

        } else {
            // Default table name
            LogUtils.w(clazz.getSimpleName() + ": table tableName not specified, using the default one based on class name");

            String className = clazz.getSimpleName();
            char c[] = className.toCharArray();
            c[0] = Character.toLowerCase(c[0]);
            className = new String(c);
            result = className.replaceAll("([A-Z])", "_$1").toLowerCase();
        }

        doAndNotifyAll(this, () -> tableName = result);
    }


    /**
     * Determine the parent entity and eventually add this entity to its children
     */
    private void loadParent() {
        EntityObject<? super T> parent = null;
        Class<? super T> superClass = clazz.getSuperclass();
        Collection<Class<?>> classes = db.getEntityClasses();

        while (superClass != null && superClass != Object.class && parent == null) {
            // We need to check if the current class is one of the mapped classes,
            // because there could be non-entity classes between the child and the
            // parent entities.

            if (classes.contains(superClass)) {
                parent = db.getEntity(superClass);
            }

            // Go up in the class hierarchy (which can be, as explained before,
            // different from entity hierarchy.
            superClass = superClass.getSuperclass();
        }

        if (parent != null) {
            parent.children.add(this);
        }

        EntityObject<? super T> result = parent;
        doAndNotifyAll(this, () -> this.parent = result);
    }


    /**
     * Determine the discriminator value
     */
    private void loadDiscriminatorValue() {
        DiscriminatorValue annotation = clazz.getAnnotation(DiscriminatorValue.class);
        Object value = annotation == null ? null : annotation.value();
        doAndNotifyAll(this, () -> discriminatorValue = value);
    }


    /**
     * Determine the relationships fields
     *
     * Each field is considered a relationship one if it is annotated with {@link OneToOne},
     * {@link OneToMany}, {@link ManyToMany} or {@link ManyToMany}
     */
    private void loadRelationships() {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(OneToOne.class) ||
                    field.isAnnotationPresent(OneToMany.class) ||
                    field.isAnnotationPresent(ManyToOne.class) ||
                    field.isAnnotationPresent(ManyToMany.class)) {

                doAndNotifyAll(this, () -> relationships.add(field));
            }
        }
    }


    /**
     * Get the unique columns sets (both the unique columns specified in the @Table annotation)
     *
     * @return list of unique columns sets
     * @see Table#uniqueConstraints()
     */
    private Collection<Collection<BaseColumnObject>> getMultipleUniqueColumns() {
        Collection<Collection<BaseColumnObject>> result = new ArrayList<>();

        // Single unique column constraints
        Columns columns = new Columns(this, this.columns.getColumnsContainers());

        // Multiple unique columns constraints (their consistence must be checked later, when all the tables have their columns assigned)
        Table table = clazz.getAnnotation(Table.class);

        if (table != null) {
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
            for (EntityObject<? extends T> child : children) {
                Collection<Collection<BaseColumnObject>> childConstrains = child.getMultipleUniqueColumns();
                result.addAll(childConstrains);
            }
        }

        return result;
    }


    /**
     * Load entity columns
     *
     * By normal columns are intended the one originated from a {@link Column} annotated field.
     *
     * The join columns are derived from:
     *  -   Fields annotated with {@link OneToOne} that are owning side
     *  -   Fields annotated with {@link ManyToOne}
     *
     * @throws InvalidConfigException in case of multiple column declaration
     */
    public void setupColumns() {
        // Normal and join columns
        Field[] allFields = clazz.getDeclaredFields();

        for (Field field : allFields) {
            if (field.isAnnotationPresent(Column.class)) {
                columns.addAll(Columns.entityFieldToColumns(db, this, field));

            } else if (field.isAnnotationPresent(OneToOne.class)) {
                OneToOne annotation = field.getAnnotation(OneToOne.class);

                if (!annotation.mappedBy().isEmpty())
                    continue;

                columns.addAll(Columns.entityFieldToColumns(db, this, field));

            } else if (field.isAnnotationPresent(ManyToOne.class)) {
                columns.addAll(Columns.entityFieldToColumns(db, this, field));
            }

            // Fields annotated with @OneToMany and @ManyToMany are skipped because they don't lead to new columns.
            // In fact, those annotations should only map the existing table columns to the join table ones.
        }

        waitWhile(columns, () -> columns.mappingStatus.get() != 0);
        LogUtils.i("[Entity \"" + getName() + "\"] own columns mapped");


        // Parent inherited primary keys (in case of JOINED inheritance strategy)
        if (parent != null && parent.inheritanceType == InheritanceType.JOINED) {
            waitWhile(parent.columns, () -> !parent.columns.joinedColumnsInherited.get());
            columns.addAll(parent.columns.getPrimaryKeys());
        }

        doAndNotifyAll(columns, () -> columns.joinedColumnsInherited.set(true));
        LogUtils.i("[Entity \"" + getName() + "\"] JOINED inherited columns added");


        // Children columns (in case of SINGLE_TABLE inheritance strategy)
        if (inheritanceType == InheritanceType.SINGLE_TABLE) {
            for (EntityObject child : children) {
                waitWhile(child, () -> !child.columns.singleTableColumnsInherited.get());
                columns.addAll(child.columns);
            }
        }

        doAndNotifyAll(columns, () -> columns.singleTableColumnsInherited.set(true));
        LogUtils.i("[Entity \"" + getName() + "\"] SINGLE_TABLE inherited columns added");


        // Discriminator column
        if (children.size() != 0) {
            if (!clazz.isAnnotationPresent(Inheritance.class))
                throw new InvalidConfigException("Class " + getName() + " doesn't have @Inheritance annotation");

            if (!clazz.isAnnotationPresent(DiscriminatorColumn.class))
                throw new InvalidConfigException("Class " + getName() + " has @Inheritance annotation but not @DiscriminatorColumn");

            DiscriminatorColumn discriminatorColumnAnnotation = clazz.getAnnotation(DiscriminatorColumn.class);

            if (discriminatorColumnAnnotation == null) {
                // Security check. Normally not reachable
                throw new MappingException("Class " + getName() + " doesn't have @DiscriminatorColumn annotation");
            }

            if (discriminatorColumnAnnotation.name().isEmpty())
                throw new InvalidConfigException("Class " + getName() + ": empty discriminator column");

            discriminatorColumn = columns.getNamesMap().get(discriminatorColumnAnnotation.name());

            if (discriminatorColumn == null)
                throw new InvalidConfigException("Class " + getName() + ": discriminator column " + discriminatorColumnAnnotation.name() + " not found");

            doAndNotifyAll(discriminatorColumn, () -> discriminatorColumn.nullable = false);

            // Fix the discriminator value type
            for (EntityObject<? extends T> child : children) {
                doAndNotifyAll(child, () -> {
                    assert child.discriminatorValue != null;

                    switch (discriminatorColumnAnnotation.discriminatorType()) {
                        case INTEGER:
                            child.discriminatorValue = Integer.valueOf((String) child.discriminatorValue);
                            break;
                    }
                });
            }
        }

        // Discriminator value
        if (parent != null && discriminatorValue == null) {
            throw new InvalidConfigException("Class " + getName() + " doesn't have @DiscriminatorValue annotation");
        }
    }


    /**
     * Get field of a class given its name.
     * The field is already set as accessible using {@link Field#setAccessible(boolean)}.
     *
     * @param fieldName     field name
     * @return accessible field
     * @throws MappingException if there is no field in the class with the specified name
     */
    public Field getField(String fieldName) {
        return getField(clazz, fieldName);
    }


    /**
     * Get field of a class given its name.
     * The field is already set as accessible using {@link Field#setAccessible(boolean)}.
     *
     * @param clazz         class the field belongs to
     * @param fieldName     field name
     *
     * @return accessible field
     *
     * @throws MappingException if there is no field in the class with the specified name
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
     * @return SQL query (null if no table should be created)
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
     * @param primaryKeys       collection of primary keys
     * @return SQL statement (null if the SQL statement is not needed in the main query)
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
     * @param uniqueColumns     unique columns groups
     * @return SQL statement (null if the SQL statement is not needed in the main query)
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
     * Get the foreign keys SQL constraints to be inserted in the create table query.
     * This method is used to create the foreign keys of a table directly associated to an entity.
     *
     * Example:
     * FOREIGN KEY (column_1, column_2) REFERENCES referenced_table_1(referenced_column_1, referenced_column_2),
     * FOREIGN KEY (column_3, column_4) REFERENCES referenced_table_2(referenced_column_3, referenced_column_4),
     * FOREIGN KEY (column_5, column_6) REFERENCES referenced_table_3(referenced_column_5, referenced_column_6)
     *
     * @return SQL statement (null if the SQL statement is not needed in the main query)
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

        return StringUtils.implode(constraints, obj -> obj, ", ");
    }


    /**
     * Get the inheritance SQL constraint to be inserted in the create table query
     *
     * Example: FOREIGN KEY (primary_key_1, primary_key_2)
     *          REFERENCES parent_table(primary_key_1, primary_key_2)
     *          ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
     *
     * @return SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getTableInheritanceConstraints() {
        if (parent == null)
            return null;

        EntityObject parent = this.parent;

        // Go up in hierarchy until there is a real table
        while (parent != null && !parent.realTable)
            parent = parent.parent;

        // Check if there's a real parent table (TABLE_PER_CLASS strategy would make this unknown)
        if (parent == null)
            return null;

        // Create associations
        Collection<BaseColumnObject> parentPrimaryKeys = parent.columns.getPrimaryKeys();
        String columns = StringUtils.implode(parentPrimaryKeys, obj -> obj.name, ", ");
        Propagation propagation = new Propagation(Propagation.Action.CASCADE, Propagation.Action.CASCADE);

        return "FOREIGN KEY(" + columns + ") " +
                "REFERENCES " + parent.tableName + "(" + columns + ") " +
                propagation;
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
     * @return SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getTableRelationshipsConstraints() {
        Collection<String> constraints = new ArrayList<>();

        for (ColumnsContainer container : columns.getColumnsContainers()) {
            if (container instanceof JoinColumnObject) {
                // @JoinColumn
                JoinColumnObject joinColumn = (JoinColumnObject) container;
                JoinColumn annotation = joinColumn.field.getAnnotation(JoinColumn.class);
                EntityObject<?> linkedEntity = db.getEntity(joinColumn.field.getType());

                constraints.add(
                        "FOREIGN KEY(" + joinColumn.name + ") " +
                        "REFERENCES " + linkedEntity.tableName + "(" + annotation.referencedColumnName() + ") " +
                        joinColumn.propagation
                );

            } else if (container instanceof JoinColumnsObject) {
                // @JoinColumns
                JoinColumnsObject joinColumns = (JoinColumnsObject) container;
                EntityObject<?> linkedEntity = db.getEntity(joinColumns.field.getType());

                List<String> local = new ArrayList<>();          // Local columns
                List<String> referenced = new ArrayList<>();     // Referenced columns

                for (BaseColumnObject column : joinColumns) {
                    JoinColumnObject joinColumn = (JoinColumnObject) column;
                    local.add(joinColumn.name);
                    referenced.add(joinColumn.linkedColumn.name);
                }

                constraints.add(
                        "FOREIGN KEY(" + StringUtils.implode(local, obj -> obj, ", ") + ") " +
                        "REFERENCES " + linkedEntity.tableName + "(" +
                        StringUtils.implode(referenced, obj -> obj, ", ") + ") " +
                        joinColumns.propagation
                );
            }
        }

        if (constraints.size() == 0) {
            return null;
        }

        return StringUtils.implode(constraints, obj -> obj, ", ");
    }

}
