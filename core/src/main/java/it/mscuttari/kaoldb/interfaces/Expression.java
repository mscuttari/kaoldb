package it.mscuttari.kaoldb.interfaces;

import androidx.annotation.NonNull;

/**
 * Expressions are used to create the "where" clause for queries
 */
public interface Expression {

    /**
     * Create "NOT" expression
     *
     * In case of double negation, the internal one is deleted and the retrieved expression
     * is the originally non negated one: NOT(NOT(expression)) = expression
     *
     * @return negated expression
     */
    Expression not();


    /**
     * Create "AND" expression
     *
     * @param expressions   expressions to be bound with "AND" operator
     * @return expression
     */
    Expression and(@NonNull Expression... expressions);


    /**
     * Create "OR" expression
     *
     * @param expressions   expressions to be bound with "OR" operator
     * @return expression
     */
    Expression or(@NonNull Expression... expressions);


    /**
     * Create "XOR" expression
     *
     * @param expressions   expressions to be bound with "XOR" operator
     * @return expression
     */
    Expression xor(@NonNull Expression... expressions);


    /**
     * Create "NAND" expression
     *
     * @param expressions   expressions to be bound with "NAND" operator
     * @return expression
     */
    Expression nand(@NonNull Expression... expressions);


    /**
     * Create "NOR" expression
     *
     * @param expressions   expressions to be bound with "NOR" operator
     * @return expression
     */
    Expression nor(@NonNull Expression... expressions);


    /**
     * Create "XNOR" expression
     *
     * @param expressions   expressions to be bound with "XNOR" operator
     * @return expression
     */
    Expression xnor(@NonNull Expression... expressions);

}
