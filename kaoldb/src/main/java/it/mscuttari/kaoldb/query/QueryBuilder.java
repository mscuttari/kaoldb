package it.mscuttari.kaoldb.query;

import it.mscuttari.kaoldb.DatabaseObject;

public class QueryBuilder {

    private DatabaseObject db;
    private From from;

    public QueryBuilder(DatabaseObject db) {
        this.db = db;
    }

    public From from(Class<?> entityClass, String alias) {
        return new From(db, entityClass, alias);
    }

}
