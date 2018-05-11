package it.mscuttari.kaoldb.core;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import it.mscuttari.kaoldb.interfaces.Query;

class QueryImpl<M> implements Query<M> {

    private EntityManagerImpl entityManager;
    private DatabaseObject db;
    private Class<M> resultClass;
    private String alias;
    private String sql;


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   resultClass     result objects type
     * @param   sql             SQL statement to be run
     */
    QueryImpl(EntityManagerImpl entityManager, DatabaseObject db, Class<M> resultClass, String alias, String sql) {
        this.entityManager = entityManager;
        this.db = db;
        this.resultClass = resultClass;
        this.alias = alias;
        this.sql = sql;
    }


    /**
     * Get SQL query
     *
     * @return  sql query
     */
    @Override
    public String toString() {
        return sql;
    }


    /** {@inheritDoc} */
    @Override
    public List<M> getResultList() {
        SQLiteDatabase db = entityManager.getReadableDatabase();

        Cursor c = db.rawQuery(sql, null);
        Log.e("KaolDb", DatabaseUtils.dumpCursorToString(c));
        List<M> result = new ArrayList<>(c.getCount());
        EntityObject entityObject = this.db.entities.get(resultClass);

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            result.add(PojoAdapter.cursorToObject(this.db, c, resultClass, entityObject, alias));
        }

        c.close();
        db.close();

        return result;
    }


    /** {@inheritDoc} */
    @Override
    public M getSingleResult() {
        List<M> resultList = getResultList();
        return resultList == null || resultList.size() == 0 ? null : resultList.get(0);
    }

}
