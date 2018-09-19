package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.interfaces.Expression;

/**
 * Expression implementation
 *
 * @see Expression
 */
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


    @Override
    public Expression not() {
        // Double negation: NOT(NOT(expression)) = expression
        if (operation == ExpressionType.NOT && x != null)
            return x;

        return new ExpressionImpl(ExpressionType.NOT, this, null);
    }


    @Override
    public Expression and(Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            if (expression != null)
                result = new ExpressionImpl(ExpressionType.AND, result, expression);
        }

        return result;
    }


    @Override
    public Expression or(Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            if (expression != null)
                result = new ExpressionImpl(ExpressionType.OR, result, expression);
        }

        return result;
    }


    @Override
    public Expression xor(Expression expression) {
        return this.and(expression.not()).or(this.not().and(expression));
    }


    @Override
    public Expression nand(Expression expression) {
        return this.and(expression).not();
    }


    @Override
    public Expression nor(Expression expression) {
        return this.not().and(expression.not());
    }


    @Override
    public Expression xnor(Expression expression) {
        return this.xor(expression).not();
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