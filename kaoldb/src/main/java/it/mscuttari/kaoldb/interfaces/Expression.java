package it.mscuttari.kaoldb.interfaces;

import it.mscuttari.kaoldb.exceptions.QueryException;

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
     * @return  negated expression
     * @throws  QueryException  if predicate is null
     */
    Expression not();


    /**
     * Create "AND" expression
     *
     * @param   expressions     expressions to be bound with "AND" operator
     * @return  expression
     */
    Expression and(Expression... expressions);


    /**
     * Create "OR" expression
     *
     * @param   expressions     expressions to be bound with "OR" operator
     * @return  expression
     * @throws  QueryException  if any of the predicates are null
     */
    Expression or(Expression... expressions);


    /**
     * Create "XOR" expression
     *
     * @param   expression      expression to be bound with "XOR" operator
     * @return  expression
     * @throws  QueryException  if any of the predicates are null
     */
    Expression xor(Expression expression);


    /**
     * Create "NAND" expression
     *
     * @param   expression      expression to be bound with "NAND" operator
     * @return  expression
     * @throws  QueryException  if any of the predicates are null
     */
    Expression nand(Expression expression);


    /**
     * Create "NOR" expression
     *
     * @param   expression      expression to be bound with "NOR" operator
     * @return  expression
     * @throws  QueryException  if any of the predicates are null
     */
    Expression nor(Expression expression);


    /**
     * Create "XNOR" expression
     *
     * @param   expression      expression to be bound with "XNOR" operator
     * @return  expression
     * @throws  QueryException  if any of the predicates are null
     */
    Expression xnor(Expression expression);

}
