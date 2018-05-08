package it.mscuttari.kaoldb.core;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

class QueryBuilderImpl<T> implements QueryBuilder<T> {

    private DatabaseObject db;
    private Class<T> resultClass;
    private EntityManagerImpl entityManager;
    private Root<?> from;
    private List<Expression> where;


    /**
     * Constructor
     *
     * @param   db              database object
     * @throws  QueryException  if the result class is not an entity
     */
    QueryBuilderImpl(DatabaseObject db, Class<T> resultClass, EntityManagerImpl entityManager) {
        this.db = db;
        this.resultClass = resultClass;
        this.entityManager = entityManager;
        this.where = new ArrayList<>();

        if (!db.entities.containsKey(resultClass))
            throw new QueryException("Class " + resultClass.getSimpleName() + " is not an entity");
    }


    /** {@inheritDoc} */
    @Override
    public <M> Root<M> getRoot(Class<M> entityClass, String alias) {
        return new From<>(db, entityClass, alias);
    }


    /** {@inheritDoc} */
    @Override
    public QueryBuilder<T> from(Root<?> from) {
        this.from = from;
        return this;
    }


    /** {@inheritDoc} */
    @Override
    public QueryBuilder<T> where(Expression expression) {
        where.add(expression);
        return this;
    }


    /** {@inheritDoc} */
    @Override
    public Query<T> build(String alias) {
        StringBuilder sb = new StringBuilder();
        String concat = "";

        // Select
        sb.append("SELECT ").append(getSelectClause(from, alias)).append(" ");

        // From
        sb.append(" FROM ").append(from);

        // Where
        if (where.size() > 0) {
            sb.append(" WHERE ");

            for (Expression expression : where) {
                sb.append(concat).append(expression.toString());
                concat = " AND ";
            }
        }

        return new QueryImpl<>(entityManager, db, resultClass, alias, sb.toString());
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
        EntityObject entity = db.entities.get(resultClass);
        for (ColumnObject column : entity.columns) {
            selectColumns.add(alias + entity.entityClass.getSimpleName() + "." + column.name + " AS \"" + alias + entity.entityClass.getSimpleName() + "." + column.name + "\"");
        }

        // Parents
        EntityObject parent = entity.parent;

        while (parent != null) {
            for (ColumnObject column : parent.columns) {
                selectColumns.add(alias + parent.entityClass.getSimpleName() + "." + column.name + " AS \"" + alias + parent.entityClass.getSimpleName() + "." + column.name + "\"");
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

            if (from.getTableAlias().equals(alias)) {
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
                result.add(alias + child.entityClass.getSimpleName() + "." + column.name + " AS \"" + alias + child.entityClass.getSimpleName() + "." + column.name + "\"");
            }

            List<String> recursiveResult = childrenSelectClause(child, alias);
            result.addAll(recursiveResult);
        }

        return result;
    }

}
