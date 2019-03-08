package it.mscuttari.kaoldb.core;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;

import static it.mscuttari.kaoldb.core.ConcurrencyUtils.waitWhile;

class Columns implements ColumnsContainer {

    /** Entity object */
    @NonNull protected final EntityObject<?> entity;

    /** Table columns */
    private final Collection<ColumnsContainer> columns = new HashSet<>();

    /** Columns mapped by name */
    private final Map<String, BaseColumnObject> namesMap = new HashMap<>();

    /** Primary keys of the table (subset of {@link #columns}) */
    private final Collection<BaseColumnObject> primaryKeys = new HashSet<>();

    /** Used to track the concurrent mapping. When 0, it means that all the columns have been mapped */
    public final AtomicInteger mappingStatus = new AtomicInteger(0);

    /** Whether the parent columns have been added, in case of JOINED inheritance */
    public final AtomicBoolean joinedColumnsInherited = new AtomicBoolean(false);

    /** Whether the parent columns have been added, in case of SINGLE_TABLE inheritance */
    public final AtomicBoolean singleTableColumnsInherited = new AtomicBoolean(false);


    /**
     * Default constructor
     *
     * @param entity    entity the columns belongs to
     */
    public Columns(@NonNull EntityObject<?> entity) {
        this(entity, null);
    }


    /**
     * Constructor.
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
     * Get an unmodifiable version of {@link #columns}
     *
     * @return columns
     */
    public final synchronized Collection<ColumnsContainer> getColumns() {
        return Collections.unmodifiableCollection(columns);
    }


    /**
     * Get an unmodifiable version of {@link #namesMap}
     *
     * @return column names map
     */
    public final synchronized Map<String, BaseColumnObject> getNamesMap() {
        return Collections.unmodifiableMap(namesMap);
    }


    /**
     * Get an unmodifiable version of {@link #primaryKeys}
     *
     * @return primary keys
     */
    public final synchronized Collection<BaseColumnObject> getPrimaryKeys() {
        return Collections.unmodifiableCollection(primaryKeys);
    }


    /**
     * Check if a column is already mapped
     *
     * @param o     column to search for
     * @return true if the column is already present; false otherwise
     */
    public final synchronized boolean contains(BaseColumnObject o) {
        for (BaseColumnObject column : this) {
            if (column.equals(o))
                return true;
        }

        return false;
    }


    /**
     * Add the columns contained by a column container
     *
     * @param container     columns container whose columns have to be added
     *
     * @return true if the columns have been successfully added;
     *         false if the container is null;
     *         false if the container can't be added to the tree for some reasons.
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
     * Get the columns linked to a field and add them to the columns set
     *
     * @param db        database
     * @param entity    entity the column belongs to
     * @param field     field the columns are generated from
     *
     * @return true if the columns have been successfully added; false otherwise
     *
     * @throws InvalidConfigException if any column has already been defined
     */
    public synchronized boolean add(DatabaseObject db, EntityObject<?> entity, Field field) {
        return addAll(entityFieldToColumns(db, entity, field));
    }


    /**
     * Add columns
     *
     * @param elements      columns or columns containers to be added
     * @return true if the columns have been successfully added; false otherwise
     */
    public synchronized boolean addAll(Collection<? extends ColumnsContainer> elements) {
        boolean result = true;

        for (ColumnsContainer element : elements)
            result &= add(element);

        return result;
    }


    /**
     * Add columns
     *
     * @param columns       column container whose columns have to be added
     * @return true if the columns have been successfully added; false otherwise
     */
    public synchronized boolean addAll(Columns columns) {
        return addAll(columns.columns);
    }


    /**
     * Check that the column to be added are not already mapped
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
     * Convert column field to single or multiple column objects
     *
     * Fields annotated with {@link Column} or {@link JoinColumn} will lead to a
     * {@link Collection} populated with just one element.
     * Fields annotated with {@link JoinColumns} or {@link JoinTable} will lead to a
     * {@link Collection} populated with multiple elements according to the join
     * columns number
     *
     * @param db        database
     * @param entity    entity the column belongs to
     * @param field     class field
     *
     * @return column objects collection
     *
     * @throws InvalidConfigException if there is no column annotation
     */
    public static Collection<ColumnsContainer> entityFieldToColumns(DatabaseObject db, EntityObject<?> entity, Field field) {
        Collection<ColumnsContainer> result = new HashSet<>();

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

        } else {
            // No annotation found
            throw new InvalidConfigException("No column annotation found for field " + field.getName());
        }

        return result;
    }


    /**
     * Get the columns SQL statement to be inserted in the table creation query.
     * Example: column 1 INTEGER UNIQUE, column 2 TEXT, column 3 REAL NOT NULL
     *
     * @return SQL statement (null if the SQL statement is not needed in the main query)
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
     * Iterator to be used to navigate the columns tree and get only the leaves, that indeed are
     * the real columns.
     */
    static class ColumnsIterator implements Iterator<BaseColumnObject> {

        private final Stack<Iterator<? extends ColumnsContainer>> stack = new Stack<>();
        private BaseColumnObject next;


        /**
         * Constructor
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
         * Prefetch the next element
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
