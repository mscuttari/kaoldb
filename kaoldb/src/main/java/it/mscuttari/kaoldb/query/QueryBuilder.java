package it.mscuttari.kaoldb.query;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.DatabaseObject;
import it.mscuttari.kaoldb.annotations.Column;

public class QueryBuilder {

    private DatabaseObject db;
    private From from;
    private List<Expression> where;

    public QueryBuilder(DatabaseObject db) {
        this.db = db;
        this.where = new ArrayList<>();
    }

    public From from(Class<?> entityClass, String alias) {
        return new From(db, entityClass, alias);
    }

    public QueryBuilder where(Expression expression) {
        where.add(expression);
        return this;
    }

}
