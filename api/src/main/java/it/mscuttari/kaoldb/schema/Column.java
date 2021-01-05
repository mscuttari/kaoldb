package it.mscuttari.kaoldb.schema;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;

/**
 * Represents a column.
 */
public final class Column {

    public final String name;
    public final Class<?> type;
    public final String defaultValue;
    public final boolean primaryKey;
    public final boolean nullable;
    public final boolean unique;

    /**
     * Constructor.
     *
     * @param name          column name
     * @param type          data class
     * @param primaryKey    whether the column is a primary key
     * @param nullable      whether the column is nullable
     * @param unique        whether the column is unique
     */
    public Column(@NonNull String name,
                  @NonNull Class<?> type,
                  String defaultValue,
                  boolean primaryKey,
                  boolean nullable,
                  boolean unique) {

        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.primaryKey = primaryKey;
        this.nullable = nullable;
        this.unique = unique;
    }

    /**
     * Get the column SQL statement to be used in the create table query.
     *
     * @return SQL statement
     */
    public String getSQL() {
        StringBuilder result = new StringBuilder();

        result.append(escape(name));
        result.append(" ").append(classToDbType(type));

        if (!nullable) {
            result.append(" NOT NULL");
        }

        if (unique) {
            result.append(" UNIQUE");
        }

        if (defaultValue != null && !defaultValue.isEmpty()) {
            result.append(" DEFAULT ").append(escape(defaultValue));
        }

        return result.toString();
    }

    @CheckResult
    private static String escape(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append('"');

        int length = str.length();
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

            if (c == '"') {
                sb.append('"');
            }

            sb.append(c);
        }

        sb.append('"');

        return sb.toString();
    }

    /**
     * Get the database column type corresponding to a given Java class
     *
     * @param clazz     Java class
     * @return column type
     */
    public static String classToDbType(Class<?> clazz) {
        if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            return "INTEGER";
        } else if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return "INTEGER";
        } else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            return "INTEGER";
        } else if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            return "REAL";
        } else if (clazz.equals(double.class) || clazz.equals(Double.class)) {
            return "REAL";
        } else if (clazz.equals(String.class)) {
            return "TEXT";
        } else if (clazz.equals(Date.class) || clazz.equals(Calendar.class)) {
            return "INTEGER";
        } else if (clazz.isEnum()) {
            return "TEXT";
        } else {
            return "BLOB";
        }
    }

}
