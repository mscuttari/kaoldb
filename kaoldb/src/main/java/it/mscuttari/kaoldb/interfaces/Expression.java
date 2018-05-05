package it.mscuttari.kaoldb.interfaces;

import it.mscuttari.kaoldb.exceptions.QueryException;

public interface Expression {

    /**
     * Create "not" expression
     *
     * @return  expression
     * @throws  QueryException  if predicate is null
     */
    Expression not();


    /**
     * Create "and" expression
     *
     * @param   expression      second expression
     * @return  expression
     * @throws  QueryException  if any of the predicates are null
     */
    Expression and(Expression expression);


    /**
     * Create "or" expression
     *
     * @param   expression      second expression
     * @return  expression
     * @throws  QueryException  if any of the predicates are null
     */
    Expression or(Expression expression);

}
