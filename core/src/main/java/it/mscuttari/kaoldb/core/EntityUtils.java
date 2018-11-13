package it.mscuttari.kaoldb.core;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.UniqueConstraint;

class EntityUtils {

    private EntityUtils() {

    }


    /**
     * Generate entities mapping
     *
     * The mapping is done in three steps:
     *  1.  Get the basic data in order to establish the paternity relationships
     *  2.  Determine the columns of each table (own columns and inherited ones)
     *  3.  Check the consistency of the previously found information that can not be checked
     *      at compile time through the annotation processors and fix the join columns types
     *
     * Create a {@link EntityObject} for each class annotated with {@link Entity} and check for
     * mapping consistence
     *
     * @param   classes     collection of all classes
     * @return  map between classes and entities objects
     */
    public static Map<Class<?>, EntityObject> createEntities(Collection<Class<?>> classes) {
        Map<Class<?>, EntityObject> result = new HashMap<>();

        // First scan to get basic data
        LogUtils.v("Entities mapping: first scan to get basic data");

        for (Class<?> entityClass : classes) {
            if (result.containsKey(entityClass)) continue;
            EntityObject entity = EntityObject.entityClassToEntityObject(entityClass, classes, result);
            result.put(entityClass, entity);
        }

        // Second scan to assign static data
        LogUtils.v("Entities mapping: second scan to assign static data");

        for (EntityObject entity : result.values()) {
            entity.setupColumns();
        }

        // Third scan to check consistence
        LogUtils.v("Entities mapping: third scan to check data consistence");

        for (EntityObject entity : result.values()) {
            entity.checkConsistence(result);
        }

        return result;
    }


    /**
     * Get the SQL query to create an entity table
     *
     * The result can be used to create just the table directly linked to the provided entity.
     * Optional join tables that are related to eventual internal fields must be managed
     * separately and in a second moment (after the creation of all the normal tables).
     *
     * @param   db          database object
     * @param   entity      entity object
     *
     * @return  SQL query (null if no table should be created)
     */
    @Nullable
    public static String getTableSql(DatabaseObject db, EntityObject entity) {
        // Skip entity if doesn't require a real table
        if (!entity.realTable)
            return null;

        StringBuilder result = new StringBuilder();

        // Table name
        result.append("CREATE TABLE IF NOT EXISTS ")
                .append(entity.tableName)
                .append(" (");

        // Columns
        String columnsSql = getColumnsSql(entity.columns);

        if (columnsSql != null && !columnsSql.isEmpty()) {
            result.append(getColumnsSql(entity.columns));
        }

        // Primary keys
        String primaryKeysSql = getTablePrimaryKeysSql(entity.primaryKeys);

        if (primaryKeysSql != null && !primaryKeysSql.isEmpty()) {
            result.append(", ").append(primaryKeysSql);
        }

        // Unique keys (multiple columns)
        String uniqueKeysSql = getTableUniquesSql(entity.getMultipleUniqueColumns());

        if (uniqueKeysSql != null && !uniqueKeysSql.isEmpty()) {
            result.append(", ").append(uniqueKeysSql);
        }

        // Foreign keys
        String foreignKeysSql = getTableForeignKeysSql(db, entity);

        if (foreignKeysSql != null && !foreignKeysSql.isEmpty()) {
            result.append(", ").append(foreignKeysSql);
        }

        result.append(");");
        //result.append(") WITHOUT ROWID;");

        return result.toString();
    }


    /**
     * Get the SQL query to create a join table
     *
     * All the normal tables must have been created before running the executing the result of
     * this method, as it will create foreign keys pointing to their columns.
     *
     * @param   db      join table annotation
     * @param   field   field annotated with {@link JoinTable}
     *
     * @return  SQL query
     */
    @Nullable
    public static String getJoinTableSql(DatabaseObject db, Field field) {
        if (!field.isAnnotationPresent(JoinTable.class))
            return null;

        JoinTable annotation = field.getAnnotation(JoinTable.class);
        StringBuilder result = new StringBuilder();

        // Table name
        result.append("CREATE TABLE IF NOT EXISTS ")
                .append(annotation.name())
                .append(" (");

        // Columns
        Collection<ColumnObject> columns = ColumnObject.getJoinTableColumns(db, field);
        result.append(getColumnsSql(columns));

        // Primary keys
        String primaryKeysSql = getTablePrimaryKeysSql(columns);

        if (primaryKeysSql != null && !primaryKeysSql.isEmpty()) {
            result.append(", ").append(primaryKeysSql);
        }

        // Foreign keys
        String foreignKeysSql = getJoinTableForeignKeysSql(db, field);

        if (foreignKeysSql != null && !foreignKeysSql.isEmpty()) {
            result.append(", ").append(foreignKeysSql);
        }

        result.append(");");

        return result.toString();
    }


    /**
     * Get columns SQL statement to be inserted in the create table query
     *
     * The columns definition takes into consideration the following parameters:
     *  -   Name.
     *  -   Column definition: if specified, the following parameters are skipped.
     *  -   Type: the column type determination is based on the field type and with respect of
     *            the following associations:
     *                  int || Integer      =>  INTEGER
     *                  long || Long        =>  INTEGER
     *                  float || Float      =>  REAL
     *                  double || Double    =>  REAL
     *                  String              =>  TEXT
     *                  Date || Calendar    =>  INTEGER (date is stored as milliseconds from epoch)
     *                  Enum                =>  TEXT (enum constant name)
     *                  anything else       =>  BLOB
     *  -   Nullability.
     *  -   Uniqueness.
     *  -   Default value.
     *
     * Example: (column 1 INTEGER UNIQUE, column 2 REAL NOT NULL)
     *
     * @param   columns     collection of all columns
     * @return  SQL query
     */
    @Nullable
    private static String getColumnsSql(Collection<ColumnObject> columns) {
        StringBuilder result = new StringBuilder();
        String prefix = "";

        for (ColumnObject column : columns) {
            // Column name
            result.append(prefix).append(column.name);
            prefix = ", ";

            // Custom column definition
            if (!column.customColumnDefinition.isEmpty()) {
                result.append(" ").append(column.customColumnDefinition);
                continue;
            }

            // Column type
            Class<?> fieldType = column.type;

            if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
                result.append(" INTEGER");
            } else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
                result.append(" INTEGER");
            } else if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
                result.append(" REAL");
            } else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
                result.append(" REAL");
            } else if (fieldType.equals(String.class)) {
                result.append(" TEXT");
            } else if (fieldType.equals(Date.class) || fieldType.equals(Calendar.class)) {
                result.append(" INTEGER");
            } else if (fieldType.isEnum()) {
                result.append(" TEXT");
            } else {
                result.append(" BLOB");
            }

            // Nullable
            if (!column.nullable) {
                result.append(" NOT NULL");
            }

            // Unique
            if (column.unique) {
                result.append(" UNIQUE");
            }

            // Default value
            if (!column.defaultValue.isEmpty()) {
                result.append(" DEFAULT ").append(column.defaultValue);
            }
        }

        return result.length() == 0 ? null : result.toString();
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
    private static String getTablePrimaryKeysSql(Collection<ColumnObject> primaryKeys) {
        if (primaryKeys == null || primaryKeys.size() == 0)
            return null;

        StringBuilder result = new StringBuilder();
        String prefix = "PRIMARY KEY(";

        for (ColumnObject column : primaryKeys) {
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
    private static String getTableUniquesSql(Collection<Collection<ColumnObject>> uniqueColumns) {
        if (uniqueColumns == null || uniqueColumns.size() == 0)
            return null;

        StringBuilder result = new StringBuilder();
        boolean empty = true;

        for (Collection<ColumnObject> uniqueSet : uniqueColumns) {
            if (uniqueSet.size() == 0)
                continue;

            if (!empty) result.append(", ");
            empty = false;
            result.append("UNIQUE(");

            String prefixInternal = "UNIQUE(";

            for (ColumnObject column : uniqueSet) {
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
     * See {@link #getJoinTableForeignKeysSql(DatabaseObject, Field)} to do the same for a join table.
     *
     * Example:
     * FOREIGN KEY (column_1, column_2) REFERENCES referenced_table_1(referenced_column_1, referenced_column_2),
     * FOREIGN KEY (column_3, column_4) REFERENCES referenced_table_2(referenced_column_3, referenced_column_4),
     * FOREIGN KEY (column_5, column_6) REFERENCES referenced_table_3(referenced_column_5, referenced_column_6)
     *
     * @param   db          database object
     * @param   entity      entity object
     *
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private static String getTableForeignKeysSql(DatabaseObject db, EntityObject entity) {
        Collection<String> constraints = new ArrayList<>();

        // Inheritance
        String inheritanceSql = getTableInheritanceConstraints(entity);

        if (inheritanceSql != null && !inheritanceSql.isEmpty())
            constraints.add(inheritanceSql);

        // Relationships
        String relationshipsSql = getTableRelationshipsConstraints(db, entity);

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
     * @param   entity      entity object
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private static String getTableInheritanceConstraints(EntityObject entity) {
        if (entity.parent == null)
            return null;

        EntityObject parent = entity.parent;

        // Go up in hierarchy until there is a real table
        while (parent != null && !parent.realTable)
            parent = parent.parent;

        // Check if there's a real parent table (TABLE_PER_CLASS strategy makes this unknown)
        if (parent == null)
            return null;

        // Normally not happening
        if (parent.primaryKeys.size() == 0)
            return null;

        // Create associations
        StringBuilder local = new StringBuilder();          // Local columns
        StringBuilder referenced = new StringBuilder();     // Referenced columns

        String separator = "";

        for (ColumnObject primaryKey : parent.primaryKeys) {
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
     * completely covered by {@link #getJoinTableForeignKeysSql(DatabaseObject, Field)}.
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
     * @param   db          database object
     * @param   entity      entity object
     *
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private static String getTableRelationshipsConstraints(DatabaseObject db, EntityObject entity) {
        boolean empty = true;
        StringBuilder result = new StringBuilder();

        for (Field field : entity.relationships) {
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


    /**
     * Get the foreign keys SQL constraints to be inserted in the create table query
     *
     * Differently from {@link #getTableForeignKeysSql(DatabaseObject, EntityObject)}, this method
     * is used for the foreign keys of a join table
     *
     * @param   db          database object
     * @param   field       field annotated with {@link JoinTable}
     *
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private static String getJoinTableForeignKeysSql(DatabaseObject db, Field field) {
        if (field == null || !field.isAnnotationPresent(JoinTable.class))
            return null;

        StringBuilder result = new StringBuilder();
        JoinTable annotation = field.getAnnotation(JoinTable.class);

        if (annotation.joinColumns().length == 0 || annotation.inverseJoinColumns().length == 0)
            return null;

        String separator = "";

        // Direct join columns
        for (JoinColumn joinColumn : annotation.joinColumns()) {
            EntityObject linkedEntity = db.getEntity(field.getDeclaringClass());

            result.append(separator)
                    .append("FOREIGN KEY(").append(joinColumn.name()).append(")")
                    .append(" REFERENCES ").append(linkedEntity.tableName).append("(").append(joinColumn.referencedColumnName()).append(")")
                    .append(" ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED");

            separator = ", ";
        }

        // Inverse join columns
        for (JoinColumn inverseJoinColumn : annotation.inverseJoinColumns()) {
            ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
            EntityObject linkedEntity = db.getEntity((Class<?>) collectionType.getActualTypeArguments()[0]);

            result.append(separator)
                    .append("FOREIGN KEY(").append(inverseJoinColumn.name()).append(")")
                    .append(" REFERENCES ").append(linkedEntity.tableName).append("(").append(inverseJoinColumn.referencedColumnName()).append(")")
                    .append(" ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED");
        }

        return result.toString();
    }

}
