package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.exceptions.QueryException;
import it.mscuttari.kaoldb.interfaces.Expression;

class ExpressionImpl implements Expression {

    private enum ExpressionType {
        NOT,
        AND,
        OR
    }


    private ExpressionType operation;
    private Expression x;
    private Expression y;


    /**
     * Constructor
     *
     * @param   operation   operation
     * @param   x           first expression
     * @param   y           second expression
     */
    ExpressionImpl(ExpressionType operation, Expression x, Expression y) {
        this.operation = operation;
        this.x = x;
        this.y = y;
    }


    /** {@inheritDoc} */
    @Override
    public Expression not() {
        return new ExpressionImpl(ExpressionType.NOT, this, null);
    }


    /** {@inheritDoc} */
    @Override
    public Expression and(Expression expression) {
        if (expression == null)
            throw new QueryException("Expression can't be null");

        return new ExpressionImpl(ExpressionType.AND, this, expression);
    }


    /** {@inheritDoc} */
    @Override
    public Expression or(Expression expression) {
        if (expression == null)
            throw new QueryException("Expression can't be null");

        return new ExpressionImpl(ExpressionType.OR, this, expression);
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
                sb.append("(").append(x.toString()).append(") AND (").append(y.toString()).append(")");
                break;

            case OR:
                sb.append("(").append(x.toString()).append(") OR (").append(y.toString()).append(")");
                break;
        }

        return sb.toString();
    }

}