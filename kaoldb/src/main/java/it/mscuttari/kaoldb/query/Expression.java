package it.mscuttari.kaoldb.query;

import it.mscuttari.kaoldb.exceptions.QueryException;

public class Expression {

    private enum ExpressionType {
        NOT,
        AND,
        OR
    }

    private ExpressionType operation;
    private Predicate x;
    private Predicate y;


    /**
     * Constructor
     *
     * @param   operation   operation
     * @param   x           first predicate
     * @param   y           second predicate
     */
    protected Expression(ExpressionType operation, Predicate x, Predicate y) {
        this.operation = operation;
        this.x = x;
        this.y = y;
    }


    /**
     * Create "not" expression
     *
     * @param   predicate   predicate
     * @return  expression
     * @throws  QueryException  if predicate is null
     */
    public static Expression not(Predicate predicate) {
        if (predicate == null)
            throw new QueryException("Predicate can't be null");

        return new Expression(ExpressionType.NOT, predicate, null);
    }


    /**
     * Create "and" expression
     *
     * @param   x       first predicate
     * @param   y       second predicate
     *
     * @return  expression
     *
     * @throws  QueryException  if any of the predicates are null
     */
    public static Expression and(Predicate x, Predicate y) {
        if (x == null || y == null)
            throw new QueryException("Predicates can't be null");

        return new Expression(ExpressionType.AND, x, y);
    }


    /**
     * Create "or" expression
     *
     * @param   x       first predicate
     * @param   y       second predicate
     *
     * @return  expression
     *
     * @throws  QueryException  if any of the predicates are null
     */
    public static Expression or(Predicate x, Predicate y) {
        if (x == null || y == null)
            throw new QueryException("Predicates can't be null");

        return new Expression(ExpressionType.OR, x, y);
    }


    /**
     * Get string representation to be used in SQL query
     *
     * @return  string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        switch (operation) {
            case NOT:
                sb.append("NOT (").append(x.toString()).append(")");
                break;

            case AND:
                sb.append("(").append(x.toString()).append(") AND (").append(y.toString());
                break;

            case OR:
                sb.append("(").append(x.toString()).append(") OR (").append(y.toString());
                break;
        }

        return sb.toString();
    }

}
