package it.mscuttari.kaoldb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class TableManager {

    private TableManager() {

    }


    /**
     * Create entities
     *
     * @param   classes     List    models
     * @return  list of entities
     */
    static List<EntityObject> createEntities(List<Class<?>> classes) {
        List<EntityObject> entities = new ArrayList<>();

        // First scan to get basic data
        for (Class<?> modelClass : classes) {
            EntityObject entity = EntityObject.modelClassToTableObject(modelClass);
            entities.add(entity);
        }

        // Second scan to create inheritance relationships
        for (EntityObject entity : entities) {
            entity.searchParentAndChildren(entities);
        }

        // Third scan to assign columns
        for (EntityObject entity : entities) {
            entity.columns = entity.getColumns();
        }

        // Fourth scan to check consistence
        for (EntityObject entity : entities) {
            entity.checkConsistence(entities);
        }

        return entities;
    }


    /**
     * Get the SQL query to create a model table
     *
     * @param   entity      EntityObject         entity
     * @return  SQL query (null if no table should be created)
     */
    @Nullable
    static String getCreateTableSql(EntityObject entity) {
        // Skip entity if doesn't require a real table
        if (!entity.isRealTable()) return null;

        StringBuilder result = new StringBuilder();

        // Table name
        result.append("CREATE TABLE IF NOT EXISTS ").append(entity.tableName).append(" (");

        // Columns
        result.append(getColumnsSql(entity.getColumns()));

        // Primary keys
        String primaryKeysSql = getPrimaryKeysSql(entity.getPrimaryKeys());

        if (!primaryKeysSql.isEmpty())
            result.append(", ").append(primaryKeysSql);

        // Unique keys (multiple columns)
        String uniqueKeysSql = getUniquesSql(entity.getMultipleUniqueColumns());

        if (!uniqueKeysSql.isEmpty())
            result.append(", ").append(uniqueKeysSql);

        result.append(");");

        return result.toString();
    }


    /**
     * Get columns SQL statement to be inserted in the create table query
     *
     * Example: (column 1 INTEGER, column 2 REAL NOT NULL)
     *
     * @param   columns     List    columns
     * @return  SQL query
     */
    @NonNull
    private static String getColumnsSql(List<ColumnObject> columns) {
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
     * @param   primaryKeys     List    primary keys
     * @return  SQL query
     */
    @NonNull
    private static String getPrimaryKeysSql(List<ColumnObject> primaryKeys) {
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
     * @param   uniqueColumns       List        list of unique columns
     * @return  SQL query
     */
    @NonNull
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
