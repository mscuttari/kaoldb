package it.mscuttari.kaoldb.core;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.exceptions.PojoException;
import it.mscuttari.kaoldb.exceptions.QueryException;

/**
 * Representation of the basic properties of a column
 *
 * @see SimpleColumnObject for the implementation of a column representing a basic entity attribute
 * @see JoinColumnObject for the implementation of a column acting as foreign key
 */
abstract class BaseColumnObject implements ColumnsContainer {

    /** Database */
    @NonNull protected final DatabaseObject db;

    /** Entity the column belongs to */
    @NonNull protected final EntityObject<?> entity;

    /** Field the column is generated from */
    @NonNull public final Field field;

    /** Column name */
    @NonNull public final String name;

    /** Custom column definition */
    @Nullable public final String customColumnDefinition;

    /**
     * Column type
     *
     * This field may contain a wrong value until the entities relationships has not been
     * checked with the {@link EntityObject#checkConsistence()} method
     */
    @NonNull public Class<?> type;

    /** Nullable column property */
    public boolean nullable;

    /** Primary key column property */
    public final boolean primaryKey;

    /** Unique column property */
    public final boolean unique;

    /** Default value */
    @Nullable public final String defaultValue;


    /**
     * Constructor
     *
     * @param db                        database
     * @param entity                    entity the column belongs to
     * @param field                     field the column is generated from
     * @param name                      column name
     * @param customColumnDefinition    column custom definition
     * @param type                      column type
     * @param nullable                  nullable property
     * @param primaryKey                primary key property
     * @param unique                    unique property
     * @param defaultValue              default value
     */
    public BaseColumnObject(@NonNull DatabaseObject db,
                            @NonNull EntityObject<?> entity,
                            @NonNull Field field,
                            @NonNull String name,
                            @Nullable String customColumnDefinition,
                            @NonNull Class<?> type,
                            boolean nullable,
                            boolean primaryKey,
                            boolean unique,
                            @Nullable String defaultValue) {

        this.db                     = db;
        this.entity                 = entity;
        this.field                  = field;
        this.name                   = name;
        this.customColumnDefinition = customColumnDefinition;
        this.type                   = type;
        this.nullable               = nullable;
        this.primaryKey             = primaryKey;
        this.unique                 = unique;
        this.defaultValue           = defaultValue;
    }


    @Override
    public final String toString() {
        return name + " (" + type.getSimpleName() + ")";
    }


    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof BaseColumnObject))
            return false;

        BaseColumnObject columnObject = (BaseColumnObject) obj;
        return name.equals(columnObject.name);
    }


    @Override
    public final int hashCode() {
        return name.hashCode();
    }


    @Override
    public Iterator<BaseColumnObject> iterator() {
        return new SingleColumnIterator(this);
    }


    /**
     * Get the default name for a column
     *
     * Uppercase characters are replaced with underscore followed by the same character converted
     * to lowercase. Only the first character, if uppercase, is converted to lowercase avoiding
     * the underscore.
     *
     * Example: columnFieldName => column_field_name
     *
     * @param field     field the column is generated from
     * @return column name
     */
    protected static String getDefaultName(@NonNull Field field) {
        String fieldName = field.getName();
        char c[] = fieldName.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        fieldName = new String(c);
        return fieldName.replaceAll("([A-Z])", "_$1").toLowerCase();
    }


    /**
     * Get field value
     *
     * @param obj       object to get the value from
     * @return field value
     * @throws PojoException if the field can't be accessed
     */
    @Nullable
    public final Object getValue(Object obj) {
        if (obj == null)
            return null;

        field.setAccessible(true);

        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new PojoException(e);
        }
    }


    /**
     * Set field value
     *
     * @param obj       object containing the field to be set
     * @param value     value to be set
     *
     * @throws QueryException if the field can't be accessed
     */
    public final void setValue(Object obj, Object value) {
        field.setAccessible(true);

        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new QueryException(e);
        }
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
     * @return SQL query
     */
    public final String getSQL() {
        StringBuilder result = new StringBuilder();
        String prefix = "";

        // Column name
        result.append(prefix).append(name);

        // Custom column definition
        if (customColumnDefinition != null && !customColumnDefinition.isEmpty()) {
            result.append(" ").append(customColumnDefinition);
            return result.toString();
        }

        // Column type
        if (type.equals(int.class) || type.equals(Integer.class)) {
            result.append(" INTEGER");
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            result.append(" INTEGER");
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            result.append(" REAL");
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            result.append(" REAL");
        } else if (type.equals(String.class)) {
            result.append(" TEXT");
        } else if (type.equals(Date.class) || type.equals(Calendar.class)) {
            result.append(" INTEGER");
        } else if (type.isEnum()) {
            result.append(" TEXT");
        } else {
            result.append(" BLOB");
        }

        // Nullable
        if (!nullable) {
            result.append(" NOT NULL");
        }

        // Unique
        if (unique) {
            result.append(" UNIQUE");
        }

        // Default value
        if (defaultValue != null && !defaultValue.isEmpty()) {
            result.append(" DEFAULT ").append(defaultValue);
        }

        return result.toString();
    }


    /**
     * Fake iterator to be used to iterate on a single column
     */
    private static class SingleColumnIterator implements Iterator<BaseColumnObject> {

        private BaseColumnObject column;

        /**
         * Constructor
         *
         * @param column    column to be returned during iteration
         */
        public SingleColumnIterator(BaseColumnObject column) {
            this.column = column;
        }


        @Override
        public boolean hasNext() {
            return column != null;
        }


        @Override
        public BaseColumnObject next() {
            try {
                return column;
            } finally {
                column = null;
            }
        }

    }

}
