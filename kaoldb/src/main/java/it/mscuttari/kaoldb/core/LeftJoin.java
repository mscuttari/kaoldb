package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.interfaces.Expression;

final class LeftJoin<X, Y> extends Join<X, Y> {

    LeftJoin(DatabaseObject db, From<Y> from, Class<X> entityClass, String alias, Expression on) {
        super(db, from, entityClass, alias, "LEFT JOIN", on);
    }

}
