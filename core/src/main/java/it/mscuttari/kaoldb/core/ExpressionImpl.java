package it.mscuttari.kaoldb.core;

import java.util.Iterator;
import java.util.Stack;

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
final class ExpressionImpl implements ExpressionInt {

    public enum ExpressionType {

        NOT  ("NOT", 1),
        AND  ("AND", 2),
        OR   ("OR",  2);

        private final String operation;
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
    private ExpressionImpl(@NonNull  ExpressionType operation,
                           @NonNull  Expression x,
                           @Nullable Expression y) {

        this.operation = operation;
        this.x =  checkNotNull(x);
        this.y = operation.cardinality == 1 ? y : checkNotNull(y);
    }


    /**
     * Create "NOT" expression
     *
     * @param x     expression to be negated
     * @return expression
     */
    public static Expression not(Expression x) {
        return new ExpressionImpl(ExpressionType.NOT, x, null);
    }


    /**
     * Create "AND" expression
     *
     * @param x     first expression
     * @param y     second expression
     *
     * @return expression
     */
    public static Expression and(Expression x, Expression y) {
        return new ExpressionImpl(ExpressionType.AND, x, y);
    }


    /**
     * Create "OR" expression
     *
     * @param x     first expression
     * @param y     second expression
     *
     * @return expression
     */
    public static Expression or(Expression x, Expression y) {
        return new ExpressionImpl(ExpressionType.OR, x, y);
    }


    /**
     * Create "XOR" expression
     *
     * @param x     first expression
     * @param y     second expression
     *
     * @return expression
     */
    public static Expression xor(Expression x, Expression y) {
        return x.xor(y);
    }


    /**
     * Create "NAND" expression
     *
     * @param x     first expression
     * @param y     second expression
     *
     * @return expression
     */
    public static Expression nand(Expression x, Expression y) {
        return x.nand(y);
    }


    /**
     * Create "NOR" expression
     *
     * @param x     first expression
     * @param y     second expression
     *
     * @return expression
     */
    public static Expression nor(Expression x, Expression y) {
        return x.nor(y);
    }


    /**
     * Create "XNOR" expression
     *
     * @param x     first expression
     * @param y     second expression
     *
     * @return expression
     */
    public static Expression xnor(Expression x, Expression y) {
        return x.xnor(y);
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
            case NOT:
                return operation + " (" + x + ")";

            case AND:
            case OR:
                return "(" + x + ") " + operation + " (" + y + ")";
        }

        throw new IllegalStateException("Unknown expression type: " + operation);
    }


    @NonNull
    @Override
    public Iterator<PredicateImpl> iterator() {
        return new PredicatesIterator(this);
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
     * Iterator to be used to get the leaves of the expressions binary tree
     */
    private static class PredicatesIterator implements Iterator<PredicateImpl> {

        private final Stack<ExpressionImpl> stack = new Stack<>();
        private PredicateImpl next;


        /**
         * Constructor
         *
         * @param expression    tree root
         */
        public PredicatesIterator(ExpressionImpl expression) {
            stack.push(expression);

            while (expression.x instanceof ExpressionImpl) {
                stack.push((ExpressionImpl) expression.x);
                expression = (ExpressionImpl) expression.x;
            }

            this.next = (PredicateImpl) expression.x;
        }


        @Override
        public boolean hasNext() {
            return next != null;
        }


        @Override
        public PredicateImpl next() {
            PredicateImpl result = next;
            next = fetchNext();
            return result;
        }


        /**
         * Fetch next node
         *
         * @return next node
         */
        private PredicateImpl fetchNext() {
            if (stack.empty())
                return null;

            ExpressionImpl expression = stack.pop();

            if (expression.y instanceof PredicateImpl)
                return (PredicateImpl) expression.y;

            expression = (ExpressionImpl) expression.y;

            while (expression instanceof ExpressionImpl) {
                stack.push((ExpressionImpl) expression.x);
                expression = (ExpressionImpl) expression.x;
            }

            return (PredicateImpl) expression.x;
        }

    }

}
