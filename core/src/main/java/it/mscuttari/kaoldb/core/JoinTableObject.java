package it.mscuttari.kaoldb.core;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinTable;

/**
 * This class allows to group the columns belonging to the same join table
 *
 * @see JoinTable
 */
final class JoinTableObject implements Iterable<BaseColumnObject> {

    /** Database the table belongs to */
    private final DatabaseObject db;

    /** Field annotated with {@link JoinTable} */
    private final Field field;

    /**
     * Direct join columns.
     * @see JoinTable#joinColumns()
     */
    private final Columns directJoinColumns;

    /**
     * Inverse join columns.
     * @see JoinTable#inverseJoinColumns()
     */
    private final Columns inverseJoinColumns;


    /**
     * Direct and inverse join columns
     *
     * @see #directJoinColumns
     * @see #inverseJoinColumns
     */
    private final Columns joinColumns;


    /**
     * Constructor
     *
     * @param   db          database the table belongs to
     * @param   entity      entity that owns the relationship
     * @param   field       field the table and its columns are generated from
     */
    public JoinTableObject(DatabaseObject db, EntityObject entity, Field field) {
        this.db = db;
        this.field = field;
        this.directJoinColumns = new Columns(entity);
        this.inverseJoinColumns = new Columns(entity);
        this.joinColumns = new Columns(entity);

        JoinTable annotation = field.getAnnotation(JoinTable.class);

        for (JoinColumn directJoinColumn : annotation.joinColumns()) {
            BaseColumnObject column = new JoinColumnObject(db, entity, field, directJoinColumn);

            directJoinColumns.add(column);
            joinColumns.add(column);
        }

        for (JoinColumn inverseJoinColumn : annotation.inverseJoinColumns()) {
            BaseColumnObject column = new JoinColumnObject(db, entity, field, inverseJoinColumn);

            inverseJoinColumns.add(column);
            joinColumns.add(column);
        }
    }


    @NonNull
    @Override
    public Iterator<BaseColumnObject> iterator() {
        return new ConcatIterator<>(directJoinColumns, inverseJoinColumns);
    }



    /**
     * Get the SQL query to create a join table
     *
     * All the normal tables must have been created before running the executing the result of
     * this method, as it will create foreign keys pointing to their columns.
     *
     * @return  SQL query
     */
    @Nullable
    public String getSQL() {
        if (!field.isAnnotationPresent(JoinTable.class))
            return null;

        JoinTable annotation = field.getAnnotation(JoinTable.class);
        StringBuilder result = new StringBuilder();

        // Table name
        result.append("CREATE TABLE IF NOT EXISTS ")
                .append(annotation.name())
                .append(" (");

        // Columns
        result.append(getColumnsSql());

        // Primary keys
        String primaryKeysSql = EntityObject.getTablePrimaryKeysSql(joinColumns.getPrimaryKeys());

        if (primaryKeysSql != null && !primaryKeysSql.isEmpty()) {
            result.append(", ").append(primaryKeysSql);
        }

        // Foreign keys
        String foreignKeysSql = getJoinTableForeignKeysSql();

        if (foreignKeysSql != null && !foreignKeysSql.isEmpty()) {
            result.append(", ").append(foreignKeysSql);
        }

        result.append(");");

        return result.toString();
    }


    /**
     * Get the columns SQL statement to be inserted in the table creation query
     *
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private String getColumnsSql() {
        StringBuilder result = new StringBuilder();
        boolean empty = true;

        for (BaseColumnObject column : this) {
            if (!empty)
                result.append(", ");

            result.append(column.getSQL());
            empty = false;
        }

        return empty ? null : result.toString();
    }


    /**
     * Get the foreign keys SQL constraints to be inserted in the create table query
     *
     * Differently from {@link EntityObject#getTableForeignKeysSql()}, this method
     * is used for the foreign keys of a join table
     *
     * @return  SQL statement (null if the SQL statement is not needed in the main query)
     */
    @Nullable
    private  String getJoinTableForeignKeysSql() {
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
