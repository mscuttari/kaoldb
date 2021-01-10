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

package it.mscuttari.kaoldb.query;

import android.database.Cursor;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.lifecycle.LiveData;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import it.mscuttari.kaoldb.ConcurrentSession;
import it.mscuttari.kaoldb.LogUtils;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;
import it.mscuttari.kaoldb.mapping.DatabaseObject;
import it.mscuttari.kaoldb.mapping.EntityObject;
import it.mscuttari.kaoldb.mapping.FieldColumnObject;
import it.mscuttari.kaoldb.mapping.Relationship;

import static it.mscuttari.kaoldb.mapping.Relationship.RelationshipType.MANY_TO_MANY;
import static it.mscuttari.kaoldb.mapping.Relationship.RelationshipType.MANY_TO_ONE;
import static it.mscuttari.kaoldb.mapping.Relationship.RelationshipType.ONE_TO_MANY;
import static it.mscuttari.kaoldb.mapping.Relationship.RelationshipType.ONE_TO_ONE;

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
     * Constructor.
     *
     * @param db                database
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
     * Get SQL query.
     *
     * @return SQL query
     */
    @NonNull
    @Override
    public String toString() {
        return sql;
    }

    @NonNull
    @Override
    public synchronized List<M> getResults() {
        LogUtils.d("[Database \"" + db.getName() + "\"] " + sql);

        entityManager.dbHelper.open();

        // Iterate among the rows and convert them to POJOs
        try (Cursor c = entityManager.dbHelper.select(sql, null)) {
            // Prepare a result list of the same size of the cursor rows amount
            // (it's just a small performance improvement done in order to prevent the collection rescaling)
            List<M> result = new ArrayList<>(c.getCount());

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                M object = db.getEntity(resultClass).parseCursor(c, alias);

                // This collection represent the tasks which will be concurrently executed in order
                // to create the queries to eagerly load the many to one and one to one relationships
                ConcurrentSession<Pair<Field, Query>> eagerLoads = getEagerLoadQueries(object);

                // While the queries are built, create the lazy collections
                createLazyCollections(object);

                // Run the queries and assign its result to the object field
                eagerLoads.waitForAll();

                for (Pair<Field, Query> eagerLoad : eagerLoads) {
                    eagerLoad.first.set(object, eagerLoad.second.getSingleResult());
                }

                result.add(object);
            }

            return result;

        } catch (Exception e) {
            throw new QueryException(e);

        } finally {
            entityManager.dbHelper.close();
        }
    }

    @Override
    public M getSingleResult() {
        List<M> resultList = getResults();

        if (resultList.isEmpty()) {
            return null;
        }

        return resultList.get(0);
    }

    @NonNull
    @Override
    public LiveData<List<M>> getLiveResults() {
        Collection<EntityObject<?>> observed = new ArraySet<>();

        EntityObject<?> entity = db.getEntity(resultClass);

        // The entities in this stack will have their hierarchy tree navigated downwards
        // Add the current entity, the children entities and the entities linked by relationships
        Stack<EntityObject<?>> downStack = new Stack<>();
        downStack.push(entity);

        // The entities in this stack will have their hierarchy tree navigated upwards
        Stack<EntityObject<?>> upStack = new Stack<>();

        // The entity on its own is already added to the downStack.
        // Let's directly start from it parent, if he has one.

        if (entity.getParent() != null) {
            upStack.push(entity.getParent());
        }

        // Recursively add all the entities linked to the starting one

        while (!downStack.empty()) {
            EntityObject<?> childEntity = downStack.pop();

            observed.add(childEntity);

            for (Relationship relationship : childEntity.relationships) {
                EntityObject<?> linked = db.getEntity(relationship.linked);

                if (!observed.contains(linked)) {
                    observed.add(linked);
                    downStack.push(linked);
                }

                if (linked.getParent() != null && !observed.contains((linked.getParent()))) {
                    upStack.push(linked.getParent());
                }
            }

            for (EntityObject<?> child : childEntity.children) {
                if (!observed.contains(child)) {
                    downStack.push(child);
                }
            }

            while (!upStack.empty()) {
                EntityObject<?> parentEntity = upStack.pop();

                for (Relationship relationship : parentEntity.relationships) {
                    EntityObject<?> linked = db.getEntity(relationship.linked);

                    if (!observed.contains(linked)) {
                        observed.add(linked);
                        downStack.push(linked);
                    }

                    if (linked.getParent() != null && !observed.contains(linked.getParent())) {
                        upStack.push(linked.getParent());
                    }
                }

                if (parentEntity.getParent() != null && !observed.contains(parentEntity.getParent())) {
                    upStack.add(parentEntity.getParent());
                }
            }
        }

        LiveQuery<M> liveQuery = new LiveQuery<>(this, observed);
        entityManager.registerLiveQuery(liveQuery);
        liveQuery.setValue(getResults());

        return liveQuery;
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
    private ConcurrentSession<Pair<Field, Query>> getEagerLoadQueries(M object) {
        ConcurrentSession<Pair<Field, Query>> concurrentSession = new ConcurrentSession<>();

        if (object == null) {
            // Security check
            return concurrentSession;
        }

        EntityObject<? super M> entity = db.getEntity((Class<M>) object.getClass());

        while (entity != null) {
            for (Relationship relationship : entity.relationships) {
                // Skip if the relationship type is not a one to one or many to one
                if (relationship.type != ONE_TO_ONE && relationship.type != MANY_TO_ONE)
                    continue;

                // Run the query in a different thread
                EntityObject<? super M> currentEntity = entity;

                concurrentSession.submit(() -> {
                    // Create the query
                    QueryBuilder<?> qb = entityManager.getQueryBuilder(relationship.linked);

                    // Create a fake property to be used for the join
                    Property<? super M, ?> property = new SingleProperty<>(currentEntity.clazz, relationship.linked, relationship.field);

                    // Create the join
                    Root leftRoot = qb.getRoot(currentEntity.clazz);
                    Root rightRoot = qb.getRoot(relationship.linked);
                    Root join = leftRoot.join(rightRoot, property);

                    Expression where = null;

                    for (FieldColumnObject primaryKey : currentEntity.columns.getPrimaryKeys()) {
                        SingleProperty primaryKeyProperty = new SingleProperty<>(currentEntity.clazz, primaryKey.type, primaryKey.field);
                        Expression primaryKeyEquality = leftRoot.eq(primaryKeyProperty, primaryKey.getValue(object));
                        where = where == null ? primaryKeyEquality : where.and(primaryKeyEquality);
                    }

                    // Build the query
                    Query<?> query = qb.from(join).where(where).build(rightRoot);
                    return new Pair<>(relationship.field, query);
                });
            }

            // Load the fields of the parent entities
            entity = entity.getParent();
        }

        return concurrentSession;
    }

    /**
     * Create lazy collections for {@link OneToMany} and {@link ManyToMany} annotated fields.
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

                for (FieldColumnObject primaryKey : current.columns.getPrimaryKeys()) {
                    SingleProperty primaryKeyProperty = new SingleProperty<>(current.clazz, primaryKey.type, primaryKey.field);
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

                    ((Collection) fieldValue).addAll(query.getResults());
                }
            }

            current = current.getParent();
        }
    }

}
