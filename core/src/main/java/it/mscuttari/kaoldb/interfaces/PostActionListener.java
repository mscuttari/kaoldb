package it.mscuttari.kaoldb.interfaces;

/**
 * @param   <M>     object class
 */
public interface PostActionListener<M> {

    /**
     * Method called by the entity manager after the action has been performed
     *
     * @param   obj     object of interest for the action
     */
    void run(M obj);

}
