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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import it.mscuttari.kaoldb.ConcatIterator;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.LogUtils;
import it.mscuttari.kaoldb.StringUtils;

import static it.mscuttari.kaoldb.ConcurrentSession.doAndNotifyAll;
import static it.mscuttari.kaoldb.StringUtils.escape;
import static it.mscuttari.kaoldb.StringUtils.implode;
import static it.mscuttari.kaoldb.mapping.Propagation.Action.CASCADE;

/**
 * This class allows to group the columns belonging to the same join table.
 *
 * @see JoinTable
 */
final class JoinTableObject implements Iterable<BaseColumnObject> {

    /** Database the table belongs to */
    @NonNull private final DatabaseObject db;

    /** Entity that owns the relationship */
    @NonNull private final EntityObject<?> entity;

    /** Field annotated with {@link JoinTable} */
    @NonNull private final Field field;

    /**
     * Direct join columns.
     *
     * @see JoinTable#joinColumns()
     */
    private final Columns directJoinColumns;

    /**
     * Inverse join columns.
     *
     * @see JoinTable#inverseJoinColumns()
     */
    private final Columns inverseJoinColumns;

    /**
     * Direct and inverse join columns.
     *
     * @see #directJoinColumns
     * @see #inverseJoinColumns
     */
    private final Columns joinColumns;

    /**
     * Constructor.
     *
     * @param db        database
     * @param entity    entity that owns the relationship
     * @param field     field the table and its columns are generated from
     */
    public JoinTableObject(@NonNull DatabaseObject db,
                            @NonNull EntityObject<?> entity,
                            @NonNull Field field) {

        this.db                 = db;
        this.entity             = entity;
        this.field              = field;
        this.directJoinColumns  = new Columns(entity);
        this.inverseJoinColumns = new Columns(entity);
        this.joinColumns        = new Columns(entity);
    }

    /**
     * Start the mapping process.
     */
    public void map() {
        JoinTable annotation = field.getAnnotation(JoinTable.class);

        LogUtils.d("[Table \"" + annotation.name() + "\"] adding direct join columns");

        for (JoinColumn directJoinColumn : annotation.joinColumns()) {
            BaseColumnObject column = new JoinColumnObject(db, entity, field, directJoinColumn);

            doAndNotifyAll(this, () -> {
                directJoinColumns.add(column);
                joinColumns.add(column);
            });

            column.map();
        }

        LogUtils.d("[Table \"" + annotation.name() + "\"] adding inverse join columns");

        for (JoinColumn inverseJoinColumn : annotation.inverseJoinColumns()) {
            BaseColumnObject column = new JoinColumnObject(db, entity, field, inverseJoinColumn);

            doAndNotifyAll(this, () -> {
                inverseJoinColumns.add(column);
                joinColumns.add(column);
            });

            column.map();
        }
    }

    public void waitUntilMapped() {
        for (BaseColumnObject column : directJoinColumns) {
            column.waitUntilMapped();
        }

        for (BaseColumnObject column : inverseJoinColumns) {
            column.waitUntilMapped();
        }
    }

    @NonNull
    @Override
    public Iterator<BaseColumnObject> iterator() {
        return new ConcatIterator<>(directJoinColumns, inverseJoinColumns);
    }

    /**
     * Get the SQL query to create a join table.
     *
     * <p>All the normal tables must have been created before running the executing the result of
     * this method, as it will create foreign keys pointing to their columns.</p>
     *
     * @return SQL query
     */
    public String getSQL() {
        JoinTable annotation = field.getAnnotation(JoinTable.class);
        StringBuilder result = new StringBuilder();

        // Table name
        result.append("CREATE TABLE IF NOT EXISTS ")
                .append(escape(annotation.name()))
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
     * Get the columns SQL statement to be inserted in the table creation query.
     *
     * @return SQL statement (<code>null</code> if the SQL statement is not needed in the main query)
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
     * Get the foreign keys SQL constraints to be inserted in the create table query.
     *
     * @return SQL statement
     */
    private String getJoinTableForeignKeysSql() {
        Collection<String> constraints = new ArrayList<>(2);

        List<String> local = new ArrayList<>();         // Local columns
        List<String> referenced = new ArrayList<>();    // Referenced columns

        JoinTable annotation = field.getAnnotation(JoinTable.class);
        Propagation propagation = new Propagation(CASCADE, CASCADE);

        // Direct join columns
        EntityObject<?> directJoinEntity = db.getEntity(annotation.joinClass());

        for (BaseColumnObject column : directJoinColumns) {
            JoinColumnObject joinColumn = (JoinColumnObject) column;
            local.add(joinColumn.name);
            referenced.add(joinColumn.linkedColumn.name);
        }

        constraints.add(
                "FOREIGN KEY (" + implode(local, StringUtils::escape, ", ") + ") " +
                "REFERENCES " + escape(directJoinEntity.tableName) + " (" +
                implode(referenced, StringUtils::escape, ", ") + ") " +
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
                "FOREIGN KEY (" + implode(local, StringUtils::escape, ", ") + ") " +
                "REFERENCES " + escape(inverseJoinEntity.tableName) + " (" +
                implode(referenced, StringUtils::escape, ", ") + ") " +
                propagation
        );

        return implode(constraints, obj -> obj, ", ");
    }

}
