package it.mscuttari.kaoldb.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.MappingException;

import static it.mscuttari.kaoldb.core.ConcurrencyUtils.doAndNotifyAll;
import static it.mscuttari.kaoldb.core.ConcurrencyUtils.waitWhile;

/**
 * This class allows to group the columns belonging to the same join table
 *
 * @see JoinTable
 */
final class JoinTableObject implements Iterable<BaseColumnObject> {

    /** Database the table belongs to */
    @NonNull private final DatabaseObject db;

    /** Field annotated with {@link JoinTable} */
    @NonNull private final Field field;

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
     * @param db        database the table belongs to
     * @param entity    entity that owns the relationship
     * @param field     field the table and its columns are generated from
     */
    private JoinTableObject(@NonNull DatabaseObject db,
                            @NonNull EntityObject<?> entity,
                            @NonNull Field field) {

        this.db                 = db;
        this.field              = field;
        this.directJoinColumns  = new Columns(entity);
        this.inverseJoinColumns = new Columns(entity);
        this.joinColumns        = new Columns(entity);
    }



    /**
     * Create the JoinTableObject linked to a field annotated with {@link JoinTable}
     * and start the mapping process.
     *
     * @param db        database the table belongs to
     * @param entity    entity that owns the relationship
     * @param field     field the table and its columns are generated from
     *
     * @return join table object
     *
     * @throws MappingException if the field doesn't have a {@link JoinTable} annotation
     */
    public static JoinTableObject map(@NonNull DatabaseObject db,
                                      @NonNull EntityObject<?> entity,
                                      @NonNull Field field) {

        JoinTableObject result = new JoinTableObject(db, entity, field);

        JoinTable annotation = field.getAnnotation(JoinTable.class);

        if (annotation == null) {
            throw new MappingException("[Entity \"" + entity.getName() + "\"] @JoinTable annotation not found on field \"" + field.getName() + "\"");
        }

        LogUtils.d("[Table \"" + annotation.name() + "\"] adding direct join columns");

        for (JoinColumn directJoinColumn : annotation.joinColumns()) {
            BaseColumnObject column = JoinColumnObject.map(db, entity, field, directJoinColumn);

            doAndNotifyAll(result, () -> {
                result.directJoinColumns.add(column);
                result.joinColumns.add(column);
            });
        }

        LogUtils.d("[Table \"" + annotation.name() + "\"] adding inverse join columns");

        for (JoinColumn inverseJoinColumn : annotation.inverseJoinColumns()) {
            BaseColumnObject column = JoinColumnObject.map(db, entity, field, inverseJoinColumn);

            doAndNotifyAll(result, () -> {
                result.inverseJoinColumns.add(column);
                result.joinColumns.add(column);
            });
        }

        waitWhile(entity.columns, () -> entity.columns.mappingStatus.get() != 0);

        return result;
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
     * @return SQL query
     */
    public String getSQL() {
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
        result.append(", ").append(foreignKeysSql);

        result.append(");");

        return result.toString();
    }


    /**
     * Get the columns SQL statement to be inserted in the table creation query
     *
     * @return SQL statement (null if the SQL statement is not needed in the main query)
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
     * @return SQL statement
     */
    private String getJoinTableForeignKeysSql() {
        Collection<String> constraints = new ArrayList<>(2);

        List<String> local = new ArrayList<>();         // Local columns
        List<String> referenced = new ArrayList<>();    // Referenced columns

        JoinTable annotation = field.getAnnotation(JoinTable.class);
        Propagation propagation = new Propagation(Propagation.Action.CASCADE, Propagation.Action.CASCADE);

        // Direct join columns
        EntityObject<?> directJoinEntity = db.getEntity(annotation.joinClass());

        for (BaseColumnObject column : directJoinColumns) {
            JoinColumnObject joinColumn = (JoinColumnObject) column;
            local.add(joinColumn.name);
            referenced.add(joinColumn.linkedColumn.name);
        }

        constraints.add(
                "FOREIGN KEY (" + StringUtils.implode(local, obj -> obj, ", ") + ") " +
                "REFERENCES " + directJoinEntity.tableName + " (" +
                StringUtils.implode(referenced, obj -> obj, ", ") + ") " +
                propagation
        );

        local.clear();
        referenced.clear();

        // Inverse join columns
        EntityObject<?> inverseJoinEntity = db.getEntity(annotation.inverseJoinClass());

        for (BaseColumnObject column : inverseJoinColumns) {
            JoinColumnObject joinColumn = (JoinColumnObject) column;
            local.add(joinColumn.name);
            referenced.add(joinColumn.linkedColumn.name);
        }

        constraints.add(
                "FOREIGN KEY (" + StringUtils.implode(local, obj -> obj, ", ") + ") " +
                "REFERENCES " + inverseJoinEntity.tableName + " (" +
                StringUtils.implode(referenced, obj -> obj, ", ") + ") " +
                propagation
        );

        return StringUtils.implode(constraints, obj -> obj, ", ");
    }

}
