package it.mscuttari.kaoldb.core;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

/**
 * @param   <M>     result objects class
 */
class QueryImpl<M> implements Query<M> {

    private final EntityManagerImpl entityManager;
    private final DatabaseObject db;
    private final Class<M> resultClass;
    private final String alias;
    private final String sql;


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   resultClass     result objects type
     * @param   sql             SQL statement to be run
     */
    QueryImpl(EntityManagerImpl entityManager, DatabaseObject db, Class<M> resultClass, String alias, String sql) {
        this.entityManager = entityManager;
        this.db = db;
        this.resultClass = resultClass;
        this.alias = alias;
        this.sql = sql;
    }


    /**
     * Get SQL query
     *
     * @return  sql query
     */
    @Override
    public String toString() {
        return sql;
    }


    @Override
    public synchronized List<M> getResultList() {
        entityManager.dbHelper.open();
        Cursor c = entityManager.dbHelper.select(sql, null);

        // Prepare a result list of the same size of the cursor rows amount
        // (it's just a small performance improvement done in order to prevent the collection rescaling)
        List<M> result = new ArrayList<>(c.getCount());

        // Map the cursor columns
        Map<String, Integer> cursorMap = getCursorColumnMap(c);

        // This collection represent the tasks which will be concurrently executed in order
        // to eagerly load the many to one and one to one relationships
        Collection<Future<?>> futures = new ArrayList<>();

        // Iterate among the rows and convert them to POJOs
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            M object = PojoAdapter.cursorToObject(this.db, c, cursorMap, resultClass, alias);
            futures.addAll(loadEagerData(object));
            createLazyCollections(object);
            result.add(object);
        }

        // Wait for all the data to be retrieved
        try {
            for (Future future : futures) {
                future.get();
            }

        } catch (Exception e) {
            for (Future future : futures) {
                future.cancel(true);
            }

            throw new QueryException(e);

        } finally {
            c.close();
            entityManager.dbHelper.close();
        }

        return result;
    }


    @Override
    public M getSingleResult() {
        List<M> resultList = getResultList();
        return resultList == null || resultList.size() == 0 ? null : resultList.get(0);
    }


    /**
     * Create a {@link Map} between each cursor column name and its column index
     *
     * Required to work with column names containing a dot, such as tableName.columnName
     * In fact, the default {@link SQLiteCursor} <a href="http://androidxref.com/5.1.0_r1/xref/frameworks/base/core/java/android/database/sqlite/SQLiteCursor.java#165">implementation</a>
     * has a section aimed to fix bug 903852, but this workaround actually breaks the usage of
     * dots in column names.
     *
     * @param   c       cursor to be mapped
     * @return  {@link Map} between column name and column index
     */
    private static Map<String, Integer> getCursorColumnMap(Cursor c) {
        Map<String, Integer> map = new HashMap<>(c.getColumnCount(), 1);
        String[] columnNames = c.getColumnNames();

        for (int i=0; i < c.getColumnCount(); i++) {
            map.put(columnNames[i], i);
        }

        return map;
    }


    /**
     * Eagerly load data linked to field annotated with {@link OneToOne} or {@link ManyToOne}
     *
     * @param   object      object got from the query
     * @return  collection of futures, each of them representing the asynchronous query task
     * @throws  QueryException if the data can't be assigned to the field
     */
    private Collection<Future<?>> loadEagerData(final M object) {
        Collection<Future<?>> futures = new ArrayList<>();

        if (object == null)
            return futures;

        ExecutorService executorService = KaolDB.getInstance().getExecutorService();

        EntityObject entity = db.getEntity(object.getClass());
        Collection<Field> usedFields = new HashSet<>();

        while (entity != null) {
            for (Field field : entity.relationships) {
                // Skip if field has already been used
                if (usedFields.contains(field))
                    continue;

                // Skip if the relationship type is not a one to one or many to one
                if (!field.isAnnotationPresent(OneToOne.class) &&
                        !field.isAnnotationPresent(ManyToOne.class))
                    continue;

                // Add the field to the used ones
                usedFields.add(field);

                // Run the query in a different thread
                EntityObject currentEntity = entity;

                Future<?> future = executorService.submit(() -> {
                    // Create the query
                    Class<?> linkedClass = field.getType();
                    QueryBuilder<?> qb = entityManager.getQueryBuilder(linkedClass);

                    // Create a fake property to be used for the join
                    Property property = new SingleProperty<>(currentEntity.entityClass, linkedClass, field);

                    // Create the join
                    Root<?> root = qb.getRoot(currentEntity.entityClass, "source");
                    Root<?> join = root.innerJoin(linkedClass, "destination", property);

                    Expression where = null;

                    for (BaseColumnObject primaryKey : currentEntity.columns.getPrimaryKeys()) {
                        SingleProperty primaryKeyProperty = new SingleProperty<>(currentEntity.entityClass, primaryKey.type, primaryKey.field);
                        Expression primaryKeyEquality = root.eq(primaryKeyProperty, primaryKey.getValue(object));
                        where = where == null ? primaryKeyEquality : where.and(primaryKeyEquality);
                    }

                    // Build the query
                    Query<?> query = qb.from(join).where(where).build("destination");

                    try {
                        // Assign the query result to the object field
                        field.set(object, query.getSingleResult());

                    } catch (IllegalAccessException e) {
                        throw new QueryException(e);
                    }
                });

                futures.add(future);
            }

            // Load the fields of the parent entities
            entity = entity.parent;
        }

        return futures;
    }


    /**
     * Create lazy collections for {@link OneToMany} and {@link ManyToMany} annotated fields
     *
     * @param   object          object got from the query
     * @throws  QueryException  if the lazy field can't be accessed
     */
    @SuppressWarnings("unchecked")
    private void createLazyCollections(M object) {
        if (object == null)
            return;

        EntityObject entity = db.getEntity(object.getClass());
        Collection<Field> usedFields = new HashSet<>();

        for (Field field : entity.relationships) {
            // Skip if field has already been used
            if (usedFields.contains(field))
                continue;

            // Skip if the relationship type is not a one to many or a many to many
            if (!field.isAnnotationPresent(OneToMany.class) &&
                    !field.isAnnotationPresent(ManyToMany.class))
                continue;

            // Add the field to the used ones
            usedFields.add(field);

            // Create the query
            Class<?> linkedClass;

            if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
                linkedClass = (Class<?>) collectionType.getActualTypeArguments()[0];
            } else {
                linkedClass = field.getType();
            }

            QueryBuilder<?> qb = entityManager.getQueryBuilder(linkedClass);
            EntityObject linkedEntity = db.getEntity(linkedClass);

            // The only fields allowed to be Collections are @OneToMany or @ManyToMany annotated ones
            OneToMany oneToManyAnnotation = field.getAnnotation(OneToMany.class);
            ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

            if (oneToManyAnnotation == null && manyToManyAnnotation == null)
                throw new QueryException("No mapping field found for relationship of field \"" + field.getName() + "\"");

            // Get the mapping field
            String mappedByFieldName = oneToManyAnnotation != null ? oneToManyAnnotation.mappedBy() : manyToManyAnnotation.mappedBy();
            Field mappedByField = linkedEntity.getField(mappedByFieldName);

            // Create a fake property to be used for the equal predicate
            Property property = new SingleProperty<>(linkedClass, entity.entityClass, mappedByField);

            // Create the join and equality constraints
            Root<?> linkedClassRoot = qb.getRoot(linkedClass, "destination");
            Root<?> join = linkedClassRoot.innerJoin(entity.entityClass, "source", property);

            Expression where = null;

            for (BaseColumnObject primaryKey : entity.columns.getPrimaryKeys()) {
                SingleProperty primaryKeyProperty = new SingleProperty<>(entity.entityClass, primaryKey.type, primaryKey.field);
                Expression primaryKeyEquality = join.eq(primaryKeyProperty, primaryKey.getValue(object));
                where = where == null ? primaryKeyEquality : where.and(primaryKeyEquality);
            }

            // Build the query to be executed when needed
            Query<?> query = qb.from(join).where(where).build("destination");

            // Get the collection instance
            Object fieldValue;

            try {
                fieldValue = field.get(object);
            } catch (IllegalAccessException e) {
                throw new QueryException(e);
            }

            // Create an appropriate lazy collection and assign it to the field
            LazyCollection<?, ?> lazyCollection;

            if (fieldValue == null) {
                lazyCollection = new LazyList<>(null, query);

            } else if (fieldValue instanceof List) {
                lazyCollection = new LazyList<>((List) fieldValue, query);

            } else if (fieldValue instanceof Set) {
                lazyCollection = new LazySet<>((Set) fieldValue, query);

            } else {
                throw new QueryException("Wrong field type. @OneToMany and @ManyToMany fields must be Lists or Sets");
            }

            try {
                field.set(object, lazyCollection);
            } catch (IllegalAccessException e) {
                throw new QueryException(e);
            }
        }
    }

}
