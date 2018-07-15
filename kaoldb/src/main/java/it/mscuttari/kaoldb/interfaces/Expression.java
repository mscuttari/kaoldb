package it.mscuttari.kaoldb.interfaces;

import it.mscuttari.kaoldb.exceptions.QueryException;

/**
 * Expressions are used to create the "where" clause for queries
 */
public interface Expression {

    /**
     * Create "not" expression
     *
     * In case of double negation, the internal one is deleted and the retrieved expression
     * is the originally non negated one: NOT(NOT(expression)) = expression
     *
     * @return  expression
     * @throws  QueryException  if predicate is null
     */
    Expression not();


    /**
     * Create "and" expression
     *
     * @param   expressions     expressions to be bound with "and" operator
     * @return  expression
     */
    Expression and(Expression... expressions);


    /**
     * Create "or" expression
     *
     * @param   expressions     expressions to be bound with "or" operator
     * @return  expression
     * @throws  QueryException  if any of the predicates are null
     */
    Expression or(Expression... expressions);

}
