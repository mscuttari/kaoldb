package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.interfaces.Expression;

abstract class Join<X, Y> extends From<X> {

    protected From<Y> from;
    protected Expression on;
    private String joinClause;

    Join(DatabaseObject db, From<Y> from, Class<X> entityClass, String alias, String joinClause, Expression on) {
        super(db, entityClass, alias);

        this.from = from;
        this.joinClause = joinClause;
        this.on = on;
    }

    @Override
    public String toString() {
        return "(" + from.toString() + " " + joinClause + " " + super.toString() + " ON " + on + ")";
    }

    private String onOld() {
        StringBuilder result = new StringBuilder();
        String separator = "";

        for (ColumnObject column : from.entity.columns) {
            if (column.field != null && column.field.getType().equals(entity.entityClass)) {
                result.append(separator).append(from.columnWithAlias(column.referencedColumnName)).append("=").append(columnWithAlias(column.name));
                separator = " AND ";
            }
        }

        return result.toString();
    }

}
