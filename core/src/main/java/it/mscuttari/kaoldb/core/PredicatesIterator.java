package it.mscuttari.kaoldb.core;

/**
 * Get the predicates (leaves) of an expressions tree
 */
final class PredicatesIterator extends LeavesIterator<ExpressionImpl> {

    /**
     * Constructor
     *
     * @param root      tree root
     */
    public PredicatesIterator(ExpressionImpl root) {
        super(root);
    }


    @Override
    public PredicateImpl next() {
        return (PredicateImpl) super.next();
    }

}
