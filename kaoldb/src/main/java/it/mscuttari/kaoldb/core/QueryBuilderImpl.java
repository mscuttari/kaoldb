package it.mscuttari.kaoldb.core;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.annotations.InheritanceType;
import it.mscuttari.kaoldb.exceptions.InvalidConfigException;
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
     * @param   db      database object
     */
    QueryBuilderImpl(DatabaseObject db, Class<T> resultClass, EntityManagerImpl entityManager) {
        this.db = db;
        this.resultClass = resultClass;
        this.entityManager = entityManager;
        this.where = new ArrayList<>();
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
    public Query<T> build() {
        StringBuilder sb = new StringBuilder();
        String concat = "";

        sb.append("SELECT * FROM ");

        Root<?> from = this.from;
        String alias = from.getTableAlias();
        EntityObject entity = db.entities.get(from.getEntityClass());

        if (entity.children.size() != 0) {
            for (EntityObject child : entity.children) {
                if (child.inheritanceType != InheritanceType.SINGLE_TABLE) {
                    Expression on = null;
                    String childAlias = alias + child.entityClass.getSimpleName();

                    for (ColumnObject primaryKey : entity.primaryKeys) {
                        if (primaryKey.field == null)
                            throw new InvalidConfigException("Primary key field not found");

                        Variable<?, ?> a = new Variable<>(db, entity, alias, new Property<>(entity.entityClass, primaryKey.type, primaryKey.field.getName()));
                        Variable<?, ?> b = new Variable<>(db, child, childAlias, new Property<>(child.entityClass, primaryKey.type, primaryKey.field.getName()));
                        Expression onChild = PredicateImpl.eq(db, a, b);
                        on = on == null ? onChild : on.and(onChild);
                    }

                    if (on == null)
                        throw new QueryException("Can't merge inherited tables");

                    from = from.leftJoin(child.entityClass, childAlias, on);
                }
            }
        }

        sb.append(from);

        if (where.size() > 0) {
            sb.append(" WHERE ");

            for (Expression expression : where) {
                sb.append(concat).append(expression.toString());
                concat = " AND ";
            }
        }

        return new QueryImpl<>(entityManager, db, resultClass, sb.toString());
    }

}
