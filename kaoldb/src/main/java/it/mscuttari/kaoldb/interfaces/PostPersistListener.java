package it.mscuttari.kaoldb.interfaces;

/**
 * @param   <M>     object class
 */
public interface PostPersistListener<M> {

    /**
     * Method called by the entity manager after an object has been persisted
     *
     * @param   obj     persisted object
     */
    void postPersist(M obj);

}
