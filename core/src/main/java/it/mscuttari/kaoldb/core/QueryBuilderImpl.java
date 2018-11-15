package it.mscuttari.kaoldb.core;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

/**
 * QueryBuilder implementation
 *
 * @see QueryBuilder
 * @param   <T>     result objects class
 */
class QueryBuilderImpl<T> implements QueryBuilder<T> {

    private final DatabaseObject db;
    private final Class<T> resultClass;
    private final EntityManagerImpl entityManager;

    private Root<?> from;
    private Expression where;


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   resultClass     class of the query result object
     * @param   entityManager   entity manager instance
     *
     * @throws  QueryException  if the result class is not an entity
     */
    QueryBuilderImpl(DatabaseObject db, Class<T> resultClass, EntityManagerImpl entityManager) {
        this.db = db;
        this.resultClass = resultClass;
        this.entityManager = entityManager;
    }


    @Override
    public <M> Root<M> getRoot(Class<M> entityClass, String alias) {
        if (alias == null || alias.isEmpty())
            throw new QueryException("Alias can't be null or empty");

        return new From<>(db, entityClass, alias);
    }


    @Override
    public QueryBuilder<T> from(Root<?> from) {
        this.from = from;
        return this;
    }


    @Override
    public QueryBuilder<T> where(Expression where) {
        this.where = where;
        return this;
    }


    @Override
    public Query<T> build(String alias) {
        if (from == null)
            throw new QueryException("\"From\" clause not set");

        Root<?> from = createJoinForPredicates(this.from, where);
        String sql = "SELECT " + getSelectClause(from, alias) + " FROM " + from;

        if (where != null)
            sql += " WHERE " + where;

        System.out.println(sql);

        return new QueryImpl<>(entityManager, db, resultClass, alias, sql);
    }


    /**
     * Create the joins according to the predicates.
     * If for example, an equality predicate is referred to another entity, a join with that
     * entity table is needed.
     *
     * @param   root    original root
     * @param   where   WHERE clause
     *
     * @return  root extended with the required joins
     */
    private static Root<?> createJoinForPredicates(Root<?> root, Expression where) {
        // No other entity involved
        if (where == null)
            return root;

        // Just a security check. This condition should never happen
        if (!(where instanceof ExpressionImpl))
            return root;

        // Iterate through the predicates of the expression
        PredicatesIterator iterator = new PredicatesIterator((ExpressionImpl) where);

        while (iterator.hasNext()) {
            PredicateImpl predicate = iterator.next();

            Object leftData = predicate.getFirstVariable().getData();

            if (leftData instanceof Property) {
                Property property = (Property) leftData;

                if (property.columnAnnotation != null && property.columnAnnotation != Column.class) {
                    String alias = Join.getJoinFullAlias(root.getAlias(), root.getEntityClass(), null);
                    root = root.innerJoin(property.fieldType, alias, property);
                }
            }
        }

        return root;
    }


    /**
     * Get the "select" clause to be used in the query
     *
     * @param   root    root
     * @param   alias   the alias of the desired result entity
     *
     * @return  "select" clause
     */
    private String getSelectClause(Root<?> root, String alias) {
        // Check alias
        checkAlias(root, alias);

        // Get columns
        List<String> selectColumns = new ArrayList<>();

        // Current entity
        EntityObject entity = db.getEntity(resultClass);
        for (ColumnObject column : entity.columns) {
            selectColumns.add(alias + entity.getName() + "." + column.name + " AS \"" + alias + entity.getName() + "." + column.name + "\"");
        }

        // Parents
        EntityObject parent = entity.parent;

        while (parent != null) {
            for (ColumnObject column : parent.columns) {
                selectColumns.add(alias + parent.getName() + "." + column.name + " AS \"" + alias + parent.getName() + "." + column.name + "\"");
            }

            parent = parent.parent;
        }

        // Children
        selectColumns.addAll(childrenSelectClause(entity, alias));

        return TextUtils.join(", ", selectColumns);
    }


    /**
     * Check if the alias exists and if it's linked to the correct class
     *
     * @param   root    root
     * @param   alias   alias
     *
     * @return  true if the alias is correct
     *
     * @throws  QueryException  if the alias has not been found or if it is linked to a class different than the result one
     */
    private boolean checkAlias(Root<?> root, String alias) {
        if (root instanceof From<?>) {
            From<?> from = (From<?>)root;

            if (from.getAlias().equals(alias)) {
                if (from.getEntityClass().equals(resultClass)) {
                    return true;
                } else {
                    throw new QueryException("Alias " + alias + " is linked to class " + from.getEntityClass().getSimpleName() + ". Expected class " + resultClass.getSimpleName());
                }
            }

            if (from instanceof Join<?, ?>) {
                if (checkAlias(((Join) from).getLeftSideRoot(), alias))
                    return true;
            }
        }

        throw new QueryException("Alias " + alias + " not found");
    }


    /**
     * Get children columns
     *
     * @param   entity      parent entity
     * @param   alias       alias to be used in the  query
     *
     * @return  columns list (each column is in the following form: aliasClassName.ColumnName
     */
    private static List<String> childrenSelectClause(EntityObject entity, String alias) {
        List<String> result = new ArrayList<>();

        for (EntityObject child : entity.children) {
            for (ColumnObject column : child.columns) {
                result.add(alias + child.getName() + "." + column.name + " AS \"" + alias + child.getName() + "." + column.name + "\"");
            }

            List<String> recursiveResult = childrenSelectClause(child, alias);
            result.addAll(recursiveResult);
        }

        return result;
    }

}
