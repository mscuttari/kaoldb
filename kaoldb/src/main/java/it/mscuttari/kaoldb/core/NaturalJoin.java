package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.interfaces.Expression;
import it.mscuttari.kaoldb.interfaces.Root;

final class NaturalJoin<X, Y> extends Join<X, Y> {

    /**
     * Constructor
     *
     * @param   db              database object
     * @param   from            first entity root to be joined
     * @param   entityClass     second entity class tot be joined
     * @param   alias           second joined entity alias
     * @param   property        first entity property to be used as bridge
     */
    NaturalJoin(DatabaseObject db, From<Y> from, Class<X> entityClass, String alias, Property<Y, X> property) {
        super(db, JoinType.INNER, from, entityClass, alias, property);
    }


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   from            first entity root to be joined
     * @param   entityClass     second entity class to be joined
     * @param   alias           second joined entity alias
     */
    NaturalJoin(DatabaseObject db, From<Y> from, Class<X> entityClass, String alias) {
        super(db, JoinType.NATURAL, from, entityClass, alias, (Expression)null);
    }

}
