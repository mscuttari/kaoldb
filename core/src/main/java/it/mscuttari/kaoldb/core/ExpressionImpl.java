package it.mscuttari.kaoldb.core;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.interfaces.Expression;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Expression implementation
 *
 * @see Expression
 */
class ExpressionImpl implements Expression, TreeNode<ExpressionImpl> {

    /**
     * {@link ExpressionType#NONE} is used just to instantiate {@link PredicateImpl}, because it
     * is managed as a particular expression
     */
    public enum ExpressionType {

        NONE ("",    1),
        NOT  ("NOT", 2),
        AND  ("AND", 2),
        OR   ("OR",  2);

        private String operation;
        public final int cardinality;

        ExpressionType(String operation, @IntRange(from = 1, to = 2) int cardinality) {
            this.operation   = operation;
            this.cardinality = cardinality;
        }

        @Override
        public String toString() {
            return operation;
        }

    }


    @NonNull  private final ExpressionType operation;
    @NonNull  private final Expression x;
    @Nullable private final Expression y;


    /**
     * Constructor
     *
     * @param operation     operation
     * @param x             first expression
     * @param y             second expression
     */
    ExpressionImpl(@NonNull  ExpressionType operation,
                   @Nullable Expression x,
                   @Nullable Expression y) {

        this.operation = operation;
        this.x = operation == ExpressionType.NONE ? x : checkNotNull(x);
        this.y = operation.cardinality == 1 ? y : checkNotNull(y);
    }


    @Override
    public ExpressionImpl getLeft() {
        return (ExpressionImpl) x;
    }


    @Override
    public ExpressionImpl getRight() {
        return (ExpressionImpl) y;
    }


    @Override
    public Expression not() {
        // Double negation: NOT(NOT(expression)) = expression
        if (operation == ExpressionType.NOT && x != null)
            return x;

        return new ExpressionImpl(ExpressionType.NOT, this, null);
    }


    @Override
    public Expression and(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = new ExpressionImpl(ExpressionType.AND, result, expression);
        }

        return result;
    }


    @Override
    public Expression or(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = new ExpressionImpl(ExpressionType.OR, result, expression);
        }

        return result;
    }


    @Override
    public Expression xor(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = result.and(expression.not()).or(result.not().and(expression));
        }

        return result;
    }


    @Override
    public Expression nand(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = result.and(expression).not();
        }

        return result;
    }


    @Override
    public Expression nor(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = result.not().and(expression.not());
        }

        return result;
    }


    @Override
    public Expression xnor(@NonNull Expression... expressions) {
        Expression result = this;

        for (Expression expression : expressions) {
            result = result.xor(expression).not();
        }

        return result;
    }


    /**
     * Get string representation to be used in SQL query
     *
     * @return string representation
     * @throws IllegalStateException if {@link #operation} is unknown
     */
    @Override
    public String toString() {
        switch (operation) {
            case NONE:
            case NOT:
                return operation + " (" + x + ")";

            case AND:
            case OR:
                return "(" + x + ") " + operation + " (" + y + ")";
        }

        throw new IllegalStateException("Unknown expression type: " + operation);
    }

}
