package it.mscuttari.kaoldb.core;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.util.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static it.mscuttari.kaoldb.core.Relationship.RelationshipType.*;

/**
 * @param <M>   result objects class
 */
class QueryImpl<M> implements Query<M> {

    @NonNull private final DatabaseObject db;
    @NonNull private final EntityManagerImpl entityManager;
    @NonNull private final Class<M> resultClass;
    @NonNull private final String alias;
    @NonNull private final String sql;


    /**
     * Constructor
     *
     * @param db                database object
     * @param entityManager     entity manager
     * @param resultClass       result objects type
     * @param alias             alias towards the query is built
     * @param sql               SQL statement to be run
     */
    QueryImpl(@NonNull DatabaseObject db,
              @NonNull EntityManagerImpl entityManager,
              @NonNull Class<M> resultClass,
              @NonNull String alias,
              @NonNull String sql) {

        this.db = db;
        this.entityManager = entityManager;
        this.resultClass = resultClass;
        this.alias = alias;
        this.sql = sql;
    }


    /**
     * Get SQL query
     *
     * @return SQL query
     */
    @NonNull
    @Override
    public String toString() {
        return sql;
    }


    @Override
    public synchronized List<M> getResultList() {
        LogUtils.d("[Database \"" + db.getName() + "\"] " + sql);

        entityManager.dbHelper.open();
        Cursor c = entityManager.dbHelper.select(sql, null);

        // Prepare a result list of the same size of the cursor rows amount
        // (it's just a small performance improvement done in order to prevent the collection rescaling)
        List<M> result = new ArrayList<>(c.getCount());

        // Map the cursor columns
        Map<String, Integer> cursorMap = getCursorColumnMap(c);

        // Iterate among the rows and convert them to POJOs
        try {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                M object = PojoAdapter.cursorToObject(this.db, c, cursorMap, resultClass, alias);

                // This collection represent the tasks which will be concurrently executed in order
                // to create the queries to eagerly load the many to one and one to one relationships
                Collection<Pair<Field, Future<Query>>> eagerLoadFutures = getEagerLoadQueries(object);

                // While the queries are built, create the lazy collections
                createLazyCollections(object);

                // Run the queries and assign its result to the object field
                for (Pair<Field, Future<Query>> queryFuture : eagerLoadFutures) {
                    queryFuture.first.set(object, queryFuture.second.get().getSingleResult());
                }

                result.add(object);
            }

        } catch (Exception e) {
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
     * @param c     cursor to be mapped
     * @return {@link Map} between column name and column index
     */
    private static Map<String, Integer> getCursorColumnMap(Cursor c) {
        Map<String, Integer> map = new HashMap<>(c.getColumnCount(), 1);
        String[] columnNames = c.getColumnNames();

        for (int i = 0; i < c.getColumnCount(); i++) {
            map.put(columnNames[i], i);
        }

        return map;
    }


    /**
     * Get the queries to be used to eagerly load data of fields with
     * {@link OneToOne} or {@link ManyToOne} annotations.
     *
     * @param object    object got from the query
     * @return collection of futures, each of them representing the asynchronous query task
     * @throws QueryException if the data can't be assigned to the field
     */
    @SuppressWarnings("unchecked")
    private Collection<Pair<Field, Future<Query>>> getEagerLoadQueries(final M object) {
        if (object == null) {
            // Security check
            return Collections.emptyList();
        }

        Collection<Pair<Field, Future<Query>>> futures = new ArrayList<>();
        ExecutorService executorService = KaolDB.getInstance().getExecutorService();

        EntityObject<? super M> entity = db.getEntity((Class<M>) object.getClass());

        while (entity != null) {
            for (Relationship relationship : entity.relationships) {
                // Skip if the relationship type is not a one to one or many to one
                if (relationship.type != ONE_TO_ONE && relationship.type != MANY_TO_ONE)
                    continue;

                // Run the query in a different thread
                EntityObject<? super M> currentEntity = entity;

                Future<Query> future = executorService.submit(() -> {
                    // Create the query
                    QueryBuilder<?> qb = entityManager.getQueryBuilder(relationship.linked);

                    // Create a fake property to be used for the join
                    Property<? super M, ?> property = new SingleProperty<>(currentEntity.clazz, relationship.linked, relationship.field);

                    // Create the join
                    Root leftRoot = qb.getRoot(currentEntity.clazz);
                    Root rightRoot = qb.getRoot(relationship.linked);
                    Root join = leftRoot.join(rightRoot, property);

                    Expression where = null;

                    for (BaseColumnObject primaryKey : currentEntity.columns.getPrimaryKeys()) {
                        SingleProperty primaryKeyProperty = new SingleProperty<>(currentEntity.clazz, primaryKey.type, primaryKey.field);
                        Expression primaryKeyEquality = leftRoot.eq(primaryKeyProperty, primaryKey.getValue(object));
                        where = where == null ? primaryKeyEquality : where.and(primaryKeyEquality);
                    }

                    // Build the query
                    return qb.from(join).where(where).build(rightRoot);
                });

                futures.add(new Pair<>(relationship.field, future));
            }

            // Load the fields of the parent entities
            entity = entity.parent;
        }

        return futures;
    }


    /**
     * Create lazy collections for {@link OneToMany} and {@link ManyToMany} annotated fields
     *
     * @param object    object got from the query
     * @throws QueryException if the lazy field can't be accessed
     */
    @SuppressWarnings("unchecked")
    private void createLazyCollections(final M object) {
        if (object == null) {
            // Security check
            return;
        }

        final EntityObject<M> entity = db.getEntity((Class<M>) object.getClass());
        EntityObject<? super M> current = entity;

        while (current != null) {
            for (Relationship relationship : current.relationships) {
                // The only fields allowed to be Collections are @OneToMany or @ManyToMany annotated ones
                if (relationship.type != ONE_TO_MANY && relationship.type != MANY_TO_MANY)
                    continue;

                // Get the mapping field
                Class<?> owningClass = relationship.getOwningClass();
                Class<?> nonOwningClass = relationship.getNonOwningClass();

                // Create a fake property to be used for the equal predicate
                Property property = new SingleProperty<>(owningClass, nonOwningClass, relationship.mappingField);

                // Create the join and equality constraints
                QueryBuilder<?> qb = entityManager.getQueryBuilder(relationship.linked);

                Root owningRoot = qb.getRoot(owningClass);
                Root nonOwningRoot = qb.getRoot(nonOwningClass);

                Root objectRoot = owningClass == entity.clazz ? owningRoot : nonOwningRoot;
                Root linkedRoot = owningClass == entity.clazz ? nonOwningRoot : owningRoot;

                Root<M> join = owningRoot.join(nonOwningRoot, property);
                Expression where = null;

                for (BaseColumnObject primaryKey : entity.columns.getPrimaryKeys()) {
                    SingleProperty primaryKeyProperty = new SingleProperty<>(entity.clazz, primaryKey.type, primaryKey.field);
                    Expression primaryKeyEquality = objectRoot.eq(primaryKeyProperty, primaryKey.getValue(object));
                    where = where == null ? primaryKeyEquality : where.and(primaryKeyEquality);
                }

                // Build the query to be executed when needed
                Query<?> query = qb.from(join).where(where).build(linkedRoot);

                // Get the collection instance
                Object fieldValue;

                try {
                    fieldValue = relationship.field.get(object);
                } catch (IllegalAccessException e) {
                    throw new QueryException(e);
                }

                if (relationship.isLazilyInitializable()) {
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
                        relationship.field.set(object, lazyCollection);
                    } catch (IllegalAccessException e) {
                        throw new QueryException(e);
                    }

                } else {
                    if (fieldValue == null)
                        throw new QueryException("Field \"" + relationship.field.getName() + "\" uninitialized");

                    ((Collection) fieldValue).addAll(query.getResultList());
                }
            }

            current = current.parent;
        }
    }

}
