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

package it.mscuttari.kaoldb.core;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;

import static it.mscuttari.kaoldb.core.ConcurrentSession.waitWhile;

class Columns implements ColumnsContainer {

    /** Entity object */
    @NonNull protected final EntityObject<?> entity;

    /** Table columns */
    private final Collection<ColumnsContainer> columns = new ArraySet<>();

    /** Columns mapped by name */
    private final Map<String, BaseColumnObject> namesMap = new ArrayMap<>();

    /** Primary keys of the table (subset of {@link #columns}) */
    private final Collection<BaseColumnObject> primaryKeys = new ArraySet<>();

    /** Used to track the concurrent mapping. When 0, it means that all the columns have been mapped */
    public final AtomicInteger mappingStatus = new AtomicInteger(0);

    /** Whether the parent columns have been added */
    public final AtomicBoolean parentColumnsInherited = new AtomicBoolean(false);


    /**
     * Default constructor.
     *
     * @param entity    entity the columns belongs to
     */
    public Columns(@NonNull EntityObject<?> entity) {
        this(entity, null);
    }


    /**
     * Constructor.<br>
     * Takes a collection of columns and sets it as the initial set.
     *
     * @param columns   columns to be added
     */
    public Columns(@NonNull EntityObject<?> entity, Collection<ColumnsContainer> columns) {
        this.entity = entity;

        if (columns != null)
            addAll(columns);
    }


    @NonNull
    @Override
    public String toString() {
        return columns.toString();
    }


    @NonNull
    @Override
    public Iterator<BaseColumnObject> iterator() {
        return new ColumnsIterator(columns);
    }


    @Override
    public synchronized void addToContentValues(@NonNull ContentValues cv, Object obj) {
        for (BaseColumnObject column : this) {
            column.addToContentValues(cv, obj);
        }
    }


    /**
     * Get an unmodifiable version of {@link #columns}.
     *
     * @return columns
     */
    public final synchronized Collection<ColumnsContainer> getColumnsContainers() {
        return Collections.unmodifiableCollection(columns);
    }


    /**
     * Get the column given its name.
     *
     * @return column (<code>null</code> if the column doesn't exist)
     */
    @Nullable
    public final synchronized BaseColumnObject get(String columnName) {
        return namesMap.get(columnName);
    }


    /**
     * Get an unmodifiable version of {@link #primaryKeys}.
     *
     * @return primary keys
     */
    public final synchronized Collection<BaseColumnObject> getPrimaryKeys() {
        return Collections.unmodifiableCollection(primaryKeys);
    }


    /**
     * Check if a column is already mapped.
     *
     * @param columnName    column name to search for
     * @return <code>true</code> if the column is already present; <code>false</code> otherwise
     */
    public final synchronized boolean contains(String columnName) {
        return namesMap.containsKey(columnName);
    }


    /**
     * Check if a column is already mapped.
     *
     * @param o     column to search for
     * @return <code>true</code> if the column is already present; <code>false</code> otherwise
     */
    public final synchronized boolean contains(BaseColumnObject o) {
        return namesMap.containsValue(o);
    }


    /**
     * Add the columns contained by a column container.
     *
     * @param container     columns container whose columns have to be added
     *
     * @return <code>true</code> if the columns have been successfully added;
     *         <code>false</code> if the container is <code>null</code>;
     *         <code>false</code> if the container can't be added to the tree for some reasons.
     *
     * @throws InvalidConfigException if any of the columns to be added are already present
     */
    public synchronized boolean add(ColumnsContainer container) {
        try {
            if (container == null)
                return false;

            // Check that the columns are not present
            checkUniqueness(container);

            for (BaseColumnObject column : container) {
                if (column == null)
                    continue;

                // Add the column name to the names map
                namesMap.put(column.name, column);

                // Check if the column is a primary key
                waitWhile(column, () -> column.primaryKey == null);

                if (column.primaryKey)
                    primaryKeys.add(column);

                LogUtils.d("[Entity \"" + entity.getName() + "\"] added column " + column);
            }

            return columns.add(container);

        } finally {
            notifyAll();
        }
    }


    /**
     * Get the columns linked to a field and add them to the columns set.
     *
     * @param db        database
     * @param entity    entity the column belongs to
     * @param field     field the columns are generated from
     *
     * @return <code>true</code> if the columns have been successfully added; <code>false</code> otherwise
     *
     * @throws InvalidConfigException if any column has already been defined
     */
    public synchronized boolean add(DatabaseObject db, EntityObject<?> entity, Field field) {
        return addAll(entityFieldToColumns(db, entity, field));
    }


    /**
     * Add columns.
     *
     * @param elements      columns or columns containers to be added
     * @return <code>true</code> if the columns have been successfully added; <code>false</code> otherwise
     */
    public synchronized boolean addAll(Collection<? extends ColumnsContainer> elements) {
        boolean result = true;

        for (ColumnsContainer element : elements)
            result &= add(element);

        return result;
    }


    /**
     * Add columns.
     *
     * @param columns       column container whose columns have to be added
     * @return <code>true</code> if the columns have been successfully added; <code>false</code> otherwise
     */
    public synchronized boolean addAll(Columns columns) {
        return addAll(columns.columns);
    }


    /**
     * Delete all the columns.
     */
    public synchronized void clear() {
        columns.clear();
        namesMap.clear();
        primaryKeys.clear();
    }


    /**
     * Check that the column to be added are not already mapped.
     *
     * @param container     columns container to search for
     * @throws InvalidConfigException if some columns have already been defined
     */
    private synchronized void checkUniqueness(ColumnsContainer container) {
        for (BaseColumnObject column : container) {
            if (this.contains(column))
                throw new InvalidConfigException("Column " + column.name + " already defined");
        }
    }


    /**
     * Convert column field to single or multiple column objects.
     *
     * <p>
     * Fields annotated with {@link Column} or {@link JoinColumn} will lead to a
     * {@link Collection} populated with just one element.<br>
     * Fields annotated with {@link JoinColumns} or {@link JoinTable} will lead to a
     * {@link Collection} populated with multiple elements according to the join
     * columns number.
     * </p>
     *
     * @param db        database
     * @param entity    entity the column belongs to
     * @param field     class field
     *
     * @return column objects collection
     */
    public static Collection<ColumnsContainer> entityFieldToColumns(DatabaseObject db, EntityObject<?> entity, Field field) {
        Collection<ColumnsContainer> result = new ArraySet<>();

        if (field.isAnnotationPresent(Column.class)) {
            result.add(SimpleColumnObject.map(db, entity, field));

        } else if (field.isAnnotationPresent(JoinColumn.class)) {
            result.add(JoinColumnObject.map(db, entity, field, field.getAnnotation(JoinColumn.class)));

        } else if (field.isAnnotationPresent(JoinColumns.class)) {
            result.add(JoinColumnsObject.map(db, entity, field));

        } else if (field.isAnnotationPresent(JoinTable.class)) {
            // Fields annotated with @JoinTable are skipped because they don't lead to new columns.
            // In fact, the annotation should only map the existing table columns to the join table
            // ones, which are created separately

            return result;
        }

        return result;
    }


    /**
     * Get the columns SQL statement to be inserted in the table creation query.
     * <p>Example: <code>"column 1" INTEGER UNIQUE, "column 2" TEXT, "column 3" REAL NOT NULL</code></p>
     *
     * @return SQL statement (<code>null</code> if the SQL statement is not needed in the main query)
     */
    @Nullable
    public final String getSQL() {
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
     * Iterator to be used to navigate the columns tree and get only the leaves, which indeed are
     * the real columns.
     */
    static class ColumnsIterator implements Iterator<BaseColumnObject> {

        private final Stack<Iterator<? extends ColumnsContainer>> stack = new Stack<>();
        private BaseColumnObject next;


        /**
         * Constructor.
         *
         * @param columns   columns collection to iterate on
         */
        public ColumnsIterator(Collection<ColumnsContainer> columns) {
            this.stack.push(columns.iterator());
            this.next = fetchNext();
        }


        @Override
        public boolean hasNext() {
            return next != null;
        }


        @Override
        public BaseColumnObject next() {
            if (next == null)
                throw new NoSuchElementException();

            BaseColumnObject next = this.next;
            this.next = fetchNext();
            return next;
        }


        /**
         * Prefetch the next element.
         *
         * @return next element
         */
        private BaseColumnObject fetchNext() {
            while (!stack.empty()) {
                // Remove depleted iterators
                if (!stack.peek().hasNext()) {
                    stack.pop();
                    continue;
                }

                // Now an iterator sits on top
                // Consume next elem from topmost iterator
                ColumnsContainer peek = stack.peek().next();

                if (peek instanceof BaseColumnObject) {
                    // Next element found
                    return (BaseColumnObject) peek;

                } else {
                    stack.push(peek.iterator());
                }
            }

            // No further elements are available, all iterators are depleted
            return null;
        }

    }

}
