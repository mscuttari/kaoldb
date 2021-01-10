/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.mapping;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import it.mscuttari.kaoldb.ConcurrentSession;
import it.mscuttari.kaoldb.LogUtils;
import it.mscuttari.kaoldb.StringUtils;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.annotations.Table;
import it.mscuttari.kaoldb.annotations.UniqueConstraint;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
import it.mscuttari.kaoldb.exceptions.MappingException;
import it.mscuttari.kaoldb.exceptions.PojoException;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.EntityManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.mscuttari.kaoldb.ConcurrentSession.doAndNotifyAll;
import static it.mscuttari.kaoldb.ConcurrentSession.waitWhile;
import static it.mscuttari.kaoldb.StringUtils.escape;
import static it.mscuttari.kaoldb.mapping.Propagation.Action.CASCADE;

/**
 * Each {@link EntityObject} maps a class annotated with the {@link Entity} annotation.
 */
public class EntityObject<T> {

    /** Database the entity belongs to */
    public final DatabaseObject db;

    /** Entity class */
    public final Class<T> clazz;

    /** Entity class instantiator */
    private final ObjectInstantiator<T> instantiator;

    /**
     * Parent entity.
     * <p><code>Null</code> if the entity has no parent.</p>
     *
     * @see #loadParent()
     */
    @Nullable
    private EntityObject<? super T> parent;

    /** Whether the parent has been determined or not during the mapping process */
    private final AtomicBoolean parentLoaded = new AtomicBoolean(false);

    /**
     * Discriminator value.
     * <p><code>Null</code> if the entity has no parent.</p>
     *
     * @see #loadDiscriminatorValue()
     */
    @Nullable
    public Object discriminatorValue;

    /**
     * Children entities.
     * <p>The children entities are determined during the parent determination process. Every time
     * an entity find its parent entity, it is also added to the children list of the latter.</p>
     *
     * @see #loadParent()
     */
    @NonNull
    public Collection<EntityObject<? extends T>> children = new ArraySet<>();

    /**
     * Table name.
     * <p><code>Null</code> if the entity doesn't require a real table.</p>
     *
     * @see #loadTableName()
     */
    @Nullable
    public String tableName;

    /**
     * Columns of the table.
     *
     * <p>At the end of the mapping process, it contains all and only the columns that build
     * the real table in the database. Therefore, it contains also the inherited primary key</p>
     *
     * @see #loadColumns()
     */
    public Columns columns = new Columns(this);

    /** Whether, during the mapping process, the parent columns have already been added or not */
    private final AtomicBoolean parentColumnsInherited = new AtomicBoolean(false);

    /**
     * Discriminator column.
     *
     * @see #loadColumns()
     */
    @Nullable
    public BaseColumnObject discriminatorColumn;

    /**
     * Relationships.
     *
     * <p>The collection contains the fields declared in {@link #clazz} (superclasses are
     * excluded) that are annotated with {@link OneToOne}, {@link OneToMany}, {@link ManyToOne}
     * or {@link ManyToMany}.</p>
     *
     * @see #loadRelationships()
     */
    public Relationships relationships = new Relationships();

    /**
     * Constructor.
     *
     * @param db        database
     * @param clazz     entity class
     */
    private EntityObject(@NonNull DatabaseObject db, @NonNull Class<T> clazz) {
        this.db = db;
        this.clazz = checkNotNull(clazz);

        Objenesis objenesis = new ObjenesisStd();
        this.instantiator = objenesis.getInstantiatorOf(clazz);
    }

    /**
     * Create the {@link EntityObject} linked to an entity class and start the mapping process.
     *
     * @param db        database
     * @param clazz     entity class
     * @param <T>       entity class
     *
     * @return entity object
     */
    public static <T> EntityObject<T> map(@NonNull DatabaseObject db, @NonNull Class<T> clazz) {
        EntityObject<T> result = new EntityObject<>(db, clazz);

        ConcurrentSession.singleTask(() -> {
            LogUtils.d("[Database \"" + db.getName() + "\"] mapping class \"" + clazz.getSimpleName() + "\"");

            result.loadTableName();
            result.loadParent();
            result.loadDiscriminatorValue();
            result.loadRelationships();
            result.loadColumns();

            // Tell the database that the entity has been completely mapped
            db.entityMapped();
        });

        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "Class: " + getName() + ", " +
                "Parent: " + (getParent() == null ? "null" : getParent().getName()) + ", " +
                "Children: {" + children.stream().map(EntityObject::getName).collect(Collectors.joining(", ")) + "}, " +
                "Columns: " + columns + ", " +
                "Primary keys: " + columns.getPrimaryKeys();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityObject))
            return false;

        EntityObject<?> entityObject = (EntityObject<?>) obj;
        return clazz.equals(entityObject.clazz);
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    /**
     * Get the simple name of the class associated to this entity.
     *
     * @return class name
     */
    public String getName() {
        return clazz.getSimpleName();
    }

    /**
     * Determine the table name.
     *
     * <p>If the table is not specified, the following policy is applied:</p>
     * <p>Uppercase characters are replaces with underscore followed by the same character converted
     * to lowercase. Only the first class name character, if uppercase, is converted to lowercase
     * while avoiding the underscore.</p>
     * <p>Example: EntityClassName => entity_class_name</p>
     *
     * @throws InvalidConfigException if the class doesn't have the @Table annotation and doesn't
     *                                expect a real table (possible only in case of
     *                                <code>TABLE_PER_CLASS</code> inheritance strategy, which is
     *                                anyway not supported)
     */
    private void loadTableName() {
        String result;
        Table annotation = clazz.getAnnotation(Table.class);

        if (annotation == null) {
            throw new InvalidConfigException("Class " + clazz.getSimpleName() + " doesn't have the @Table annotation");

        } else if (!annotation.name().isEmpty()) {
            // Get specified table name
            result = annotation.name();

        } else {
            // Default table name
            LogUtils.w("[Class \"" + clazz.getSimpleName() + "\"] table name not specified, using the default one based on class name");

            String className = clazz.getSimpleName();
            char[] c = className.toCharArray();
            c[0] = Character.toLowerCase(c[0]);
            className = new String(c);
            result = className.replaceAll("([A-Z])", "_$1").toLowerCase();
        }

        doAndNotifyAll(this, () -> tableName = result);
    }

    /**
     * Determine the parent entity and eventually add this entity to its children.
     */
    private void loadParent() {
        EntityObject<? super T> parent = null;
        Class<? super T> superClass = clazz.getSuperclass();

        while (superClass != null && superClass != Object.class && parent == null) {
            // We need to check if the current class is one of the mapped classes,
            // because there could be non-entity classes between the child and the
            // parent entities.

            if (db.contains(superClass)) {
                parent = db.getEntity(superClass);
            }

            // Go up in the class hierarchy (which can be, as explained before,
            // different from entity hierarchy).
            superClass = superClass.getSuperclass();
        }

        if (parent != null) {
            LogUtils.d("[Entity \"" + getName() + "\"] found parent \"" + parent.getName() + "\"");
            parent.addChild(this);
        }

        EntityObject<? super T> result = parent;

        doAndNotifyAll(this, () -> {
            this.parent = result;
            parentLoaded.set(true);
        });
    }

    /**
     * Add a child to the children list.
     *
     * @param child     child entity
     */
    private void addChild(EntityObject<? extends T> child) {
        doAndNotifyAll(this, () -> {
            LogUtils.d("[Entity \"" + getName() + "\"] found child \"" + child.getName() + "\"");
            children.add(child);
        });
    }

    /**
     * Block the calling thread until the parent has been determined.
     *
     * @see #loadParent()
     */
    private void waitUntilParentLoaded() {
        waitWhile(this, () -> !parentLoaded.get());
    }

    /**
     * Get the parent entity.
     * <p>The calling thread is blocked until the parent has been determined by the mapping process.</p>
     *
     * @return parent entity (<code>null</code> if the current entity has no parent)
     */
    @Nullable
    public EntityObject<? super T> getParent() {
        waitUntilParentLoaded();
        return parent;
    }

    /**
     * Determine the discriminator value.
     */
    private void loadDiscriminatorValue() {
        DiscriminatorValue annotation = clazz.getAnnotation(DiscriminatorValue.class);
        Object value = annotation == null ? null : annotation.value();
        doAndNotifyAll(this, () -> discriminatorValue = value);
    }

    /**
     * Get the child with a specific {@link #discriminatorValue}.
     *
     * @param discriminator     discriminator value of the child entity
     * @return child entity
     * @throws NoSuchElementException if there is no child with the specified discriminator value
     */
    public EntityObject<? extends T> getChild(Object discriminator) {
        if (discriminator != null) {
            for (EntityObject<? extends T> child : children) {
                // Children discriminator values should not be null by definition
                assert child.discriminatorValue != null : "Entity \"" + child.clazz.getSimpleName() + "\" should not have a null discriminator value";

                if (child.discriminatorValue.equals(discriminator)) {
                    return child;
                }
            }
        }

        throw new NoSuchElementException("Entity \"" + clazz.getSimpleName() + "\" has no child with discriminator value \"" + discriminator + "\"");
    }

    /**
     * Determine the relationships fields.
     *
     * A field is considered a relationship one if it is annotated with {@link OneToOne},
     * {@link OneToMany}, {@link ManyToMany} or {@link ManyToMany}.
     */
    private void loadRelationships() {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(OneToOne.class) ||
                    field.isAnnotationPresent(OneToMany.class) ||
                    field.isAnnotationPresent(ManyToOne.class) ||
                    field.isAnnotationPresent(ManyToMany.class)) {

                Relationship relationship = new Relationship(db, field);
                doAndNotifyAll(this, () -> relationships.add(relationship));
            }
        }
    }

    /**
     * Get the unique columns sets (both the unique columns specified in the
     * {@link Table} annotation).
     *
     * @return list of unique columns sets
     * @throws InvalidConfigException if a column with the specified name doesn't exist
     * @see Table#uniqueConstraints()
     */
    private Collection<Collection<BaseColumnObject>> getMultipleUniqueColumns() {
        Collection<Collection<BaseColumnObject>> result = new ArrayList<>();
        Columns columns = new Columns(this, this.columns.getColumnsContainers());
        Table table = clazz.getAnnotation(Table.class);

        if (table != null) {
            UniqueConstraint[] uniqueConstraints = table.uniqueConstraints();

            for (UniqueConstraint uniqueConstraint : uniqueConstraints) {
                Collection<BaseColumnObject> multipleUniqueColumns = new ArrayList<>(uniqueConstraint.columnNames().length);

                for (String columnName : uniqueConstraint.columnNames()) {
                    BaseColumnObject column = columns.get(columnName);

                    if (column == null) {
                        throw new InvalidConfigException("Unique constraint: column \"" + columnName + "\" not found");
                    }

                    multipleUniqueColumns.add(column);
                }

                result.add(multipleUniqueColumns);
            }
        }

        return result;
    }

    /**
     * Load entity columns.
     *
     * <p>By normal columns are intended the one originated from a {@link Column} annotated field.</p>
     * <p>The join columns are derived from:
     * <ul>
     *      <li>Fields annotated with {@link OneToOne} that are the owning side</li>
     *      <li>Fields annotated with {@link ManyToOne}</li>
     * </ul></p>
     *
     * @throws InvalidConfigException in case of multiple column declaration
     */
    public void loadColumns() {
        // Normal and join columns

        for (Field field : clazz.getDeclaredFields()) {
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

        columns.map();
        columns.waitUntilMapped();

        LogUtils.i("[Entity \"" + getName() + "\"] own columns mapped");

        // Wait until all the hierarchies are determined
        Collection<EntityObject<?>> entities = db.getEntities();

        for (EntityObject<?> entity : entities) {
            entity.waitUntilParentLoaded();
        }

        // Parent inherited primary keys
        EntityObject<? super T> parent = getParent();

        if (parent != null) {
            waitWhile(parent, () -> !parent.parentColumnsInherited.get());
            columns.addAll(parent.columns.getPrimaryKeys());
        }

        doAndNotifyAll(this, () -> parentColumnsInherited.set(true));
        LogUtils.i("[Entity \"" + getName() + "\"] inherited columns added");

        // Discriminator column

        if (children.size() != 0) {
            if (!clazz.isAnnotationPresent(DiscriminatorColumn.class))
                throw new InvalidConfigException("Class " + getName() + " has no @DiscriminatorColumn");

            DiscriminatorColumn discriminatorColumnAnnotation = clazz.getAnnotation(DiscriminatorColumn.class);

            if (discriminatorColumnAnnotation == null) {
                // Security check. Normally not reachable.
                throw new MappingException("Class " + getName() + " doesn't have @DiscriminatorColumn annotation");
            }

            if (discriminatorColumnAnnotation.name().isEmpty())
                throw new InvalidConfigException("Class " + getName() + ": empty discriminator column");

            discriminatorColumn = columns.get(discriminatorColumnAnnotation.name());

            // Create the discriminator column if it doesn't exist
            if (discriminatorColumn == null) {
                discriminatorColumn = new DiscriminatorColumnObject(db, this);
                discriminatorColumn.map();
                discriminatorColumn.waitUntilMapped();

                columns.add(discriminatorColumn);
            }

            doAndNotifyAll(discriminatorColumn, () -> discriminatorColumn.nullable = false);

            // Fix the discriminator value type
            for (EntityObject<? extends T> child : children) {
                doAndNotifyAll(child, () -> {
                    assert child.discriminatorValue != null;

                    switch (discriminatorColumnAnnotation.discriminatorType()) {
                        case CHAR:
                            child.discriminatorValue = ((String) child.discriminatorValue).charAt(0);
                            break;

                        case INTEGER:
                            child.discriminatorValue = Integer.valueOf((String) child.discriminatorValue);
                            break;
                    }
                });
            }
        }

        LogUtils.d("[Entity \"" + getName() + "\"] all columns loaded");
    }

    /**
     * Get field of a class given its name.
     * <p>The returned field is already set as accessible using {@link Field#setAccessible(boolean)}.</p>
     *
     * @param fieldName     field name
     * @return accessible field
     */
    public Field getField(String fieldName) {
        return getField(clazz, fieldName);
    }

    /**
     * Get the field of a class given the field name.
     *
     * <p>
     * The returned field is already set as accessible using {@link Field#setAccessible(boolean)}.
     * </p>
     *
     * @param clazz         class the field belongs to
     * @param fieldName     field name
     *
     * @return accessible field
     *
     * @throws IllegalArgumentException if there is no field in the class with the specified name
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            field.setAccessible(true);
            return field;

        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field \"" + fieldName + "\" not found in class \"" + clazz.getSimpleName() + "\"", e);
        }
    }

    /**
     * Create an instance of the entity class.
     *
     * @return object having a class equal to {@link #clazz}
     */
    public T newInstance() {
        return instantiator.newInstance();
    }

    /**
     * Convert cursor to POJO.
     *
     * <p>The conversion automatically search for the child class according to the discriminator value.<br>
     * In fact, the method starts with a first part with the aim of going down through the hierarchy
     * tree in order to retrieve the leaf class representing the real object class.<br>
     * Then, the second part creates an instance of that class and populates its basic fields with
     * the data contained in the cursor.</p>
     *
     * @param c             cursor
     * @param alias         result class alias
     *
     * @return populated object
     *
     * @throws PojoException if the child class is not found (wrong discriminator column value) or
     *                       if it can not be instantiated
     */
    public T parseCursor(Cursor c, String alias) {
        // The result class may be abstract, so we need to determine the real class of the result,
        // which will be a subclass of the requested one.

        EntityObject<? extends T> resultEntity = this;
        String fullAlias = alias;

        // Go down to the child class.
        // Each iteration will go one step down through the hierarchy tree.

        while (resultEntity.children.size() != 0) {
            // Get the discriminator value
            assert resultEntity.discriminatorColumn != null;
            String discriminatorColumnName = fullAlias + "." + resultEntity.discriminatorColumn.name;
            int columnIndex = c.getColumnIndexOrThrow(discriminatorColumnName);
            Object discriminatorValue = null;

            if (resultEntity.discriminatorColumn.type.equals(Integer.class)) {
                discriminatorValue = c.getInt(columnIndex);
            } else if (resultEntity.discriminatorColumn.type.equals(String.class)) {
                discriminatorValue = c.getString(columnIndex);
            }

            // Determine the child class according to the discriminator value specified
            resultEntity = resultEntity.getChild(discriminatorValue);
            fullAlias = alias + resultEntity.getName();
        }

        T result = resultEntity.newInstance();

        // We are now in the leaf entity. Go back up in the hierarchy tree and populate
        // the fields of each parent entity.

        EntityObject<?> entity = resultEntity;

        while (entity != null) {
            for (BaseColumnObject column : entity.columns) {
                if (column.hasRelationship()) {
                    // Relationships are loaded separately
                    continue;
                }

                // The parents and the children entities have their entity name appended
                // to their root aliases.

                Object fieldValue = column.parseCursor(c, entity.clazz.equals(clazz) ? alias: alias + entity.getName());
                column.setValue(result, fieldValue);
            }

            entity = entity.getParent();
        }

        return result;
    }

    /**
     * Prepare the data to be saved in a particular entity table.
     *
     * <p>{@link ContentValues#size()} must be checked before saving the data in the database.
     * If zero, no data needs to be saved and, if not skipped, the
     * {@link SQLiteDatabase#insert(String, String, ContentValues)} method would throw an exception.</p>
     *
     * @param entityManager     entity manager
     * @param obj               object to be converted
     *
     * @return data ready to be saved in the database
     *
     * @throws PojoException if the discriminator value has been manually set but is not
     *                       compatible with the child entity class
     * @throws PojoException if the field associated with the discriminator value can't be accessed
     */
    @NonNull
    public ContentValues toContentValues(Object obj, EntityManager entityManager) {
        ContentValues cv = new ContentValues();

        for (BaseColumnObject column : columns) {
            if (!column.isDataExisting(obj, entityManager)) {
                throw new QueryException("Object of column \"" + column.name + "\" doesn't exist in the database. Persist it first!");
            }

            column.addToContentValues(cv, obj);
        }

        return cv;
    }

    /**
     * Get the SQL query to create the entity table.
     *
     * <p>The result can be used to create just the table directly linked to the provided entity.<br>
     * Optional join tables that are related to eventual internal fields must be managed
     * separately and in a second moment (after the creation of all the normal tables).</p>
     *
     * @return SQL query for table creation
     */
    @Nullable
    public String getSQL() {
        StringBuilder result = new StringBuilder();

        // Table name
        result.append("CREATE TABLE IF NOT EXISTS ")
                .append(escape(tableName))
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

        if (!uniqueKeysSql.isEmpty()) {
            result.append(", ").append(uniqueKeysSql);
        }

        // Foreign keys
        String foreignKeysSql = getTableForeignKeysSql();

        if (foreignKeysSql != null && !foreignKeysSql.isEmpty()) {
            result.append(", ").append(foreignKeysSql);
        }

        result.append(");");

        return result.toString();
    }

    /**
     * Get primary keys SQL statement to be inserted in the create table query.
     *
     * <p>Example: <code>PRIMARY KEY("column_1", "column_2", "column_3")</code></p>
     *
     * @param primaryKeys       collection of primary keys
     * @return SQL statement (<code>null</code> if the SQL statement is not needed in the main query)
     */
    @Nullable
    public static String getTablePrimaryKeysSql(Collection<FieldColumnObject> primaryKeys) {
        if (primaryKeys == null || primaryKeys.size() == 0) {
            return null;
        }

        return "PRIMARY KEY (" +
                primaryKeys.stream()
                        .map(StringUtils::escape)
                        .collect(Collectors.joining(", ")) +
                ")";
    }

    /**
     * Get unique columns SQL statement to be inserted in the create table query.
     *
     * <p>The parameter uniqueColumns is a collection of collections because each unique constraint
     * can be made of multiple columns. For example, a collection such as
     * <code>[[column_1, column_2], [column_2, column_3, column_4]]</code> would generate the
     * statement <code>UNIQUE("column_1", "column_2"), UNIQUE("column_2", "column_3", "column_4")</code></p>
     *
     * @param uniqueColumns     unique columns groups
     * @return SQL statement
     */
    private static String getTableUniquesSql(Collection<Collection<BaseColumnObject>> uniqueColumns) {
        return uniqueColumns.stream()
                .filter(columns -> columns != null && !columns.isEmpty())
                .map(columns -> "UNIQUE (" +
                        columns.stream()
                                .map(column -> escape(column.name))
                                .collect(Collectors.joining(", ")) +
                        ")")
                .collect(Collectors.joining(", "));
    }

    /**
     * Get the foreign keys SQL constraints to be inserted in the create table query.
     *
     * <p>Example:
     * <code>
     *     FOREIGN KEY ("column_1", "column_2") REFERENCES "referenced_table_1" ("referenced_column_1", "referenced_column_2") ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
     *     FOREIGN KEY ("column_3", "column_4") REFERENCES "referenced_table_2" ("referenced_column_3", "referenced_column_4") ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
     *     FOREIGN KEY ("column_5", "column_6") REFERENCES "referenced_table_3" ("referenced_column_5", "referenced_column_6") ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
     * </code></p>
     *
     * @return SQL statement (<code>null</code> if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getTableForeignKeysSql() {
        Collection<String> constraints = new ArrayList<>();

        // Inheritance
        String inheritanceSql = getTableInheritanceConstraints();

        if (inheritanceSql != null && !inheritanceSql.isEmpty()) {
            constraints.add(inheritanceSql);
        }

        // Relationships
        String relationshipsSql = getTableRelationshipsConstraints();

        if (relationshipsSql != null && !relationshipsSql.isEmpty()) {
            constraints.add(relationshipsSql);
        }

        // Create SQL statement
        if (constraints.size() == 0) {
            return null;
        }

        return constraints.stream().collect(Collectors.joining(", "));
    }

    /**
     * Get the inheritance SQL constraint to be inserted in the create table query.
     *
     * <p>Example:
     * <code>
     *     FOREIGN KEY ("primary_key_1", "primary_key_2")
     *     REFERENCES "parent_table" ("primary_key_1", "primary_key_2")
     *     ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
     * </code></p>
     *
     * @return SQL statement (<code>null</code> if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getTableInheritanceConstraints() {
        EntityObject<? super T> parent = getParent();

        if (parent == null) {
            return null;
        }

        // Create associations
        Collection<FieldColumnObject> parentPrimaryKeys = parent.columns.getPrimaryKeys();

        String columns = parentPrimaryKeys.stream()
                .map(primaryKey -> escape(primaryKey.name))
                .collect(Collectors.joining(", "));

        Propagation propagation = new Propagation(CASCADE, CASCADE);

        return "FOREIGN KEY (" + columns + ") " +
                "REFERENCES " + escape(parent.tableName) + " (" + columns + ") " +
                propagation;
    }

    /**
     * Get the relationships SQL constraints to be inserted in the create table query.
     *
     * <p>Example:
     * <code>
     *     FOREIGN KEY ("column_1", "column_2")
     *     REFERENCES "referenced_table_1" ("referenced_column_1", "referenced_column_2")
     *     ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
     * </code><br>
     * <code>
     *     FOREIGN KEY ("column_3", "column_4")
     *     REFERENCES "referenced_table_2" ("referenced_column_3", "referenced_column_4")
     *     ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
     * </code></p>
     *
     * @return SQL statement (<code>null</code> if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getTableRelationshipsConstraints() {
        Collection<String> constraints = new ArrayList<>();

        for (ColumnsContainer container : columns.getColumnsContainers()) {
            constraints.addAll(container.accept(new RelationshipsContraintsVisitor(db)));
        }

        if (constraints.isEmpty()) {
            return null;
        }

        return constraints.stream().collect(Collectors.joining(", "));
    }

    private static class RelationshipsContraintsVisitor implements ColumnsContainer.Visitor<Collection<String>> {

        private final DatabaseObject db;

        public RelationshipsContraintsVisitor(DatabaseObject db) {
            this.db = db;
        }

        @Override
        public Collection<String> visit(Columns container) {
            return Collections.emptyList();
        }

        @Override
        public Collection<String> visit(DiscriminatorColumnObject column) {
            return Collections.emptyList();
        }

        @Override
        public Collection<String> visit(JoinColumnObject column) {
            Collection<String> constraints = new ArrayList<>();

            JoinColumn annotation = column.field.getAnnotation(JoinColumn.class);
            EntityObject<?> linkedEntity = db.getEntity(column.field.getType());

            String constraint = "FOREIGN KEY (" + escape(column.name) + ") " +
                    "REFERENCES " + escape(linkedEntity.tableName) +
                    " (" + escape(annotation.referencedColumnName()) + ") " +
                    column.propagation;

            constraints.add(constraint);
            return constraints;
        }

        @Override
        public Collection<String> visit(JoinColumnsObject container) {
            Collection<String> constraints = new ArrayList<>();

            EntityObject<?> linkedEntity = db.getEntity(container.field.getType());

            List<String> local = new ArrayList<>();          // Local columns
            List<String> referenced = new ArrayList<>();     // Referenced columns

            for (BaseColumnObject column : container) {
                JoinColumnObject joinColumn = (JoinColumnObject) column;
                local.add(joinColumn.name);
                referenced.add(joinColumn.linkedColumn.name);
            }

            String constraint = "FOREIGN KEY (" + local.stream().map(StringUtils::escape).collect(Collectors.joining(", ")) + ") " +
                    "REFERENCES " + escape(linkedEntity.tableName) + " (" +
                    referenced.stream().map(StringUtils::escape).collect(Collectors.joining(", ")) + ") " +
                    container.propagation;

            constraints.add(constraint);
            return constraints;
        }

        @Override
        public Collection<String> visit(SimpleColumnObject column) {
            return Collections.emptyList();
        }
    }

}
