package it.mscuttari.kaoldb.core;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.mscuttari.kaoldb.interfaces.Query;

/**
 * @param   <M>     result objects class
 */
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


    @Override
    public List<M> getResultList() {
        SQLiteDatabase db = entityManager.getReadableDatabase();

        Cursor c = db.rawQuery(sql, null);

        List<M> result = new ArrayList<>(c.getCount());
        EntityObject entityObject = this.db.getEntityObject(resultClass);

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Map<String, Integer> cursorMap = getCursorColumnMap(c);
            result.add(PojoAdapter.cursorToObject(c, cursorMap, resultClass, entityObject, alias));
        }

        c.close();
        db.close();

        return result;
    }


    @Override
    public M getSingleResult() {
        List<M> resultList = getResultList();
        return resultList == null || resultList.size() == 0 ? null : resultList.get(0);
    }


    /**
     * Create a {@link Map} between each cursor column name and its column index
     *
     * Required to work with column names containing a dot, such as tableName.columnName
     * In fact, the default {@link SQLiteCursor} <a href="http://androidxref.com/5.1.0_r1/xref/frameworks/base/core/java/android/database/sqlite/SQLiteCursor.java#165">implementation</a>
     * has a section aimed to fix bug 903852, but this workaround actually breaks the usage of
     * dots in column names.
     *
     * @param   c       cursor to be mapped
     * @return  {@link Map} between column name and column index
     */
    private static Map<String, Integer> getCursorColumnMap(Cursor c) {
        Map<String, Integer> map = new HashMap<>(c.getColumnCount(), 1);
        String[] columnNames = c.getColumnNames();

        for (int i=0; i < c.getColumnCount(); i++) {
            map.put(columnNames[i], i);
        }

        return map;
    }

}
