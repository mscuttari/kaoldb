package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.interfaces.Expression;

final class LeftJoin<X, Y> extends Join<X, Y> {

    /**
     * Constructor
     *
     * @param   db              database object
     * @param   from            first entity root to be joined
     * @param   entityClass     second entity class to be joined
     * @param   alias           second joined entity alias
     * @param   on              "on" expression
     */
    LeftJoin(DatabaseObject db, From<Y> from, Class<X> entityClass, String alias, Expression on) {
        super(db, JoinType.LEFT, from, entityClass, alias, on);
    }

}
