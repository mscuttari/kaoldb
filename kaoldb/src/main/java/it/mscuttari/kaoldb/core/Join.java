package it.mscuttari.kaoldb.core;

import android.support.annotation.NonNull;

import it.mscuttari.kaoldb.interfaces.Expression;

abstract class Join<X, Y> extends From<X> {

    enum JoinType {
        INNER("INNER JOIN"),
        LEFT("LEFT JOIN"),
        NATURAL("NATURAL JOIN");

        private String clause;

        JoinType(String clause) {
            this.clause = clause;
        }

        @Override
        public String toString() {
            return clause;
        }
    }


    private JoinType type;
    private From<Y> from;
    private Expression on;


    /**
     * Constructor
     *
     * @param   db              database object
     * @param   type            join type
     * @param   from            first entity root to be joined
     * @param   entityClass     second entity class to be joined
     * @param   alias           second joined entity alias
     * @param   on              "on" expression
     */
    Join(DatabaseObject db, @NonNull JoinType type, From<Y> from, Class<X> entityClass, String alias, Expression on) {
        super(db, entityClass, alias);

        this.type = type;
        this.from = from;
        this.on = on;
    }


    /**
     * Get string representation to be used in query
     *
     * @return  "from" clause
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(").append(from).append(" ").append(type).append(" ").append(super.toString());
        if (on != null) sb.append(" ON ").append(on);
        sb.append(")");

        return sb.toString();
    }

}
