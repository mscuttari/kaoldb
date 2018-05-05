package it.mscuttari.kaoldb.core;

import it.mscuttari.kaoldb.exceptions.QueryException;

class PredicateImpl extends ExpressionImpl {

    private enum PredicateType {
        EQUAL,
        GT,
        GE,
        LT,
        LE
    }

    private PredicateType operation;
    private Variable x;
    private Variable y;


    /**
     * Constructor
     *
     * @param   operation   operation
     * @param   x           first variable
     * @param   y           second variable
     */
    private PredicateImpl(PredicateType operation, Variable x, Variable y) {
        super(null, null, null);

        this.operation = operation;
        this.x = x;
        this.y = y;
    }


    /**
     * Create "equals" predicate
     *
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl eq(Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.EQUAL, x, y);
    }


    /**
     * Create "greater than" predicate
     *
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl gt(Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.GT, x, y);
    }


    /**
     * Create "greater or equals than" predicate
     *
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl ge(Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.GE, x, y);
    }


    /**
     * Create "less than" predicate
     *
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl lt(Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.LT, x, y);
    }


    /**
     * Create "less or equals than" predicate
     *
     * @param   x   first variable
     * @param   y   second variable
     *
     * @return  predicate
     *
     * @throws  QueryException  if any of the variables are null
     */
    public static PredicateImpl le(Variable x, Variable y) {
        if (x == null || y == null)
            throw new QueryException("Variables can't be null");

        return new PredicateImpl(PredicateType.LE, x, y);
    }


    /**
     * Get string representation to be used in SQL query
     *
     * @return  string representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(x.toString());

        switch (operation) {
            case EQUAL:
                sb.append(" = ");
                break;

            case GT:
                sb.append(" > ");
                break;

            case GE:
                sb.append(" >= ");
                break;

            case LT:
                sb.append(" < ");
                break;

            case LE:
                sb.append(" <= ");
                break;
        }

        sb.append(y.toString());

        return sb.toString();
    }

}
