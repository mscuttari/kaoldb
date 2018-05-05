package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.interfaces.Expression;

final class InnerJoin<X, Y> extends Join<X, Y> {

    InnerJoin(DatabaseObject db, From<Y> from, Class<X> entityClass, String alias, Expression on) {
        super(db, from, entityClass, alias, "INNER JOIN", on);
    }

}
