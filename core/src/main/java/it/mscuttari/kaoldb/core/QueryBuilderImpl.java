package it.mscuttari.kaoldb.core;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Query;
import it.mscuttari.kaoldb.interfaces.QueryBuilder;
import it.mscuttari.kaoldb.interfaces.Root;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * QueryBuilder implementation
 *
 * @see QueryBuilder
 * @param <T>   result objects class
 */
class QueryBuilderImpl<T> implements QueryBuilder<T> {

    @NonNull private final EntityManagerImpl entityManager;
    @NonNull private final DatabaseObject db;
    @NonNull private final Class<T> resultClass;

    // Counter for the roots got from this query builder.
    // It is used to create an unique alias for each root.
    private int rootCounter = 0;

    private Root<?> from;
    private Expression where;


    /**
     * Constructor
     *
     * @param db                database
     * @param resultClass       class of the query result object
     * @param entityManager     entity manager
     */
    QueryBuilderImpl(@NonNull DatabaseObject db,
                     @NonNull Class<T> resultClass,
                     @NonNull EntityManagerImpl entityManager) {

        this.db = db;
        this.resultClass = checkNotNull(resultClass);
        this.entityManager = entityManager;
    }


    @Override
    public <M> Root<M> getRoot(@NonNull Class<M> entityClass) {
        return new From<>(db, this, entityClass, "a" + rootCounter++);
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
    public Query<T> build(Root<T> root) {
        if (from == null)
            throw new QueryException("\"From\" clause not set");

        Root<?> from = createJoinForPredicates(this.from, where);
        String sql = "SELECT " + getSelectClause(root) + " FROM " + from;

        if (where != null)
            sql += " WHERE " + where;

        return new QueryImpl<>(db, entityManager, resultClass, root.getAlias(), sql);
    }


    /**
     * Create the joins according to the predicates.
     * If for example, an equality predicate is referred to another entity, a join with that
     * entity table is needed.
     *
     * @param root      original root
     * @param where     "WHERE" clause
     *
     * @return root extended with the required joins
     */
    private Root<?> createJoinForPredicates(Root<?> root, Expression where) {
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
            Object leftData = predicate.x.getData();

            if (!(leftData instanceof Property))
                continue;

            Property property = (Property) leftData;

            if (property.columnAnnotation != null && property.columnAnnotation != Column.class) {
                //String alias = Join.getJoinFullAlias(root.getAlias(), root.getEntityClass(), null);
                Root<?> joinedRoot = new From<>(db, this, (Class<?>) property.fieldType, predicate.root.getAlias());
                root = root.join(joinedRoot, property);
            }
        }

        return root;
    }


    /**
     * Get the "SELECT" clause to be used in the query
     *
     * @param root      root
     *
     * @return "SELECT" clause
     */
    private String getSelectClause(Root<?> root) {
        // Check alias
        //checkAlias(root, alias);

        List<String> columns = new ArrayList<>();

        // Current entity
        EntityObject<?> entity = db.getEntity(resultClass);

        for (BaseColumnObject column : entity.columns) {
            columns.add(
                    root.getAlias() + "." + column.name +
                    " AS " +
                    "\"" + root.getAlias() + "." + column.name + "\""
            );
        }

        // Parents
        EntityObject<?> parent = entity.parent;

        while (parent != null) {
            for (BaseColumnObject column : parent.columns) {
                columns.add(
                        root.getAlias() + parent.getName() + "." + column.name +
                        " AS " +
                        "\"" + root.getAlias() + parent.getName() + "." + column.name + "\""
                );
            }

            parent = parent.parent;
        }

        // Children
        Stack<EntityObject<?>> children = new Stack<>();
        children.push(entity);

        while (!children.empty()) {
            EntityObject<?> node = children.pop();

            for (EntityObject<?> child : node.children) {
                for (BaseColumnObject column : child.columns) {
                    columns.add(root.getAlias() + child.getName() + "." + column.name +
                                " AS " +
                                "\"" + root.getAlias() + child.getName() + "." + column.name + "\""
                    );
                }

                // Depth first scan
                if (child.children.size() != 0) {
                    children.push(child);
                }
            }
        }

        return TextUtils.join(", ", columns);
    }


    /**
     * Check if the alias exists and if it's linked to the correct class
     *
     * @param root      root
     * @param alias     alias
     *
     * @return true if the alias is correct
     *
     * @throws QueryException if the alias has not been found or if it is linked to a class different than the result one
     */
    private boolean checkAlias(Root<?> root, String alias) {
        if (root.getAlias().equals(alias)) {
            if (root.getEntityClass().equals(resultClass)) {
                return true;
            } else {
                throw new QueryException("Alias " + alias + " is linked to class " + root.getEntityClass().getSimpleName() + ". Expected class: " + resultClass.getSimpleName() + ".");
            }
        }

        if (root instanceof Join<?, ?>) {
            if (checkAlias(((Join<?, ?>) root).getRightRoot(), alias))
                return true;
        }

        throw new QueryException("Alias " + alias + " not found");
    }

}
