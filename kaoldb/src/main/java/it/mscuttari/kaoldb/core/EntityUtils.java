package it.mscuttari.kaoldb.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.annotations.Entity;

class EntityUtils {

    private EntityUtils() {

    }


    /**
     * Generate entities mapping
     *
     * Create a {@link EntityObject} for each class annotated with {@link Entity} and check for
     * mapping consistence
     *
     * @param   classes     collection of all classes
     * @return  map between classes and entities objects
     */
    static Map<Class<?>, EntityObject> createEntities(Collection<Class<?>> classes) {
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
     * @param   entity      entity object
     * @return  SQL query (null if no table should be created)
     */
    @Nullable
    static String getCreateTableSql(EntityObject entity) {
        // Skip entity if doesn't require a real table
        if (!entity.realTable) return null;

        StringBuilder result = new StringBuilder();

        // Table name
        result.append("CREATE TABLE IF NOT EXISTS ").append(entity.tableName).append(" (");

        // Columns
        result.append(getColumnsSql(entity.columns));

        // Primary keys
        String primaryKeysSql = getPrimaryKeysSql(entity.primaryKeys);

        if (!primaryKeysSql.isEmpty())
            result.append(", ").append(primaryKeysSql);

        // Unique keys (multiple columns)
        String uniqueKeysSql = getUniquesSql(entity.getMultipleUniqueColumns());

        if (!uniqueKeysSql.isEmpty())
            result.append(", ").append(uniqueKeysSql);

        result.append(");");
        //result.append(") WITHOUT ROWID;");

        return result.toString();
    }


    /**
     * Get columns SQL statement to be inserted in the create table query
     *
     * Example: (column 1 INTEGER, column 2 REAL NOT NULL)
     *
     * @param   columns     collection of all columns
     * @return  SQL query
     */
    @NotNull
    private static String getColumnsSql(Collection<ColumnObject> columns) {
        StringBuilder result = new StringBuilder();
        String prefix = "";

        for (ColumnObject column : columns) {
            // Column tableName
            result.append(prefix).append(column.name);
            prefix = ", ";

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

            // TODO: default value
            // TODO: autoincrement
            // TODO: custom text
        }

        return result.toString();
    }


    /**
     * Get primary keys SQL statement to be inserted in the create table query
     *
     * Example: PRIMARY KEY(column_1, column_2, column_3)
     *
     * @param   primaryKeys     collection of primary keys
     * @return  SQL query
     */
    @NotNull
    private static String getPrimaryKeysSql(Collection<ColumnObject> primaryKeys) {
        StringBuilder result = new StringBuilder();
        boolean empty = true;
        String prefix = "PRIMARY KEY(";

        for (ColumnObject column : primaryKeys) {
            result.append(prefix).append(column.name);
            prefix = ", ";
            empty = false;
        }

        if (!empty) {
            result.append(")");
        }

        return result.toString();
    }


    /**
     * Get unique columns SQL statement to be inserted in the create table query
     *
     * Example: UNIQUE(column_1, column_2), UNIQUE(column_2, column_3, column_4)
     *
     * @param   uniqueColumns       list of unique columns
     * @return  SQL query
     */
    @NotNull
    private static String getUniquesSql(List<List<ColumnObject>> uniqueColumns) {
        StringBuilder result = new StringBuilder();
        String prefixExternal = "";

        for (List<ColumnObject> uniqueSet : uniqueColumns) {
            StringBuilder uc = new StringBuilder();
            boolean empty = true;
            String prefixInternal = "UNIQUE(";

            for (ColumnObject column : uniqueSet) {
                uc.append(prefixInternal).append(column.name);
                prefixInternal = ", ";

                empty = false;
            }

            if (empty) {
                continue;
            } else {
                uc.append(")");
            }

            result.append(prefixExternal).append(uc);
            prefixExternal = ", ";
        }

        return result.toString();
    }

}
