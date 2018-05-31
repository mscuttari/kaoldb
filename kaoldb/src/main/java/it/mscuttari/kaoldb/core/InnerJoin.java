package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.interfaces.Expression;

/**
 * @param   <X>     right side entity of the join
 * @param   <Y>     left side entity of the join
 */
final class InnerJoin<X, Y> extends Join<X, Y> {

    /**
     * Constructor
     *
     * @param   db              database object
     * @param   from            first entity root to be joined
     * @param   entityClass     second entity class tot be joined
     * @param   alias           second joined entity alias
     * @param   property        first entity property to be used as bridge
     */
    InnerJoin(DatabaseObject db, From<Y> from, Class<X> entityClass, String alias, Property<Y, X> property) {
        super(db, JoinType.INNER, from, entityClass, alias, property);
    }


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   from            first entity root to be joined
     * @param   entityClass     second entity class to be joined
     * @param   alias           second joined entity alias
     * @param   on              "on" expression
     */
    InnerJoin(DatabaseObject db, From<Y> from, Class<X> entityClass, String alias, Expression on) {
        super(db, JoinType.INNER, from, entityClass, alias, on);
    }

}