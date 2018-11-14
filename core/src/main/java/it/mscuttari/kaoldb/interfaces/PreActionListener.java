package it.mscuttari.kaoldb.interfaces;

/**
 * @param   <M>     object class
 */
public interface PreActionListener<M> {

    /**
     * Method called by the entity manager before the action is performed
     *
     * @param   obj     object of interest for the action
     */
    void run(M obj);

}
