package it.mscuttari.kaoldb.interfaces;

public interface PrePersistListener<M> {

    /**
     * Method called by the entity manager before persisting an object
     *
     * @param   obj     object which is going to be persisted
     */
    void prePersist(M obj);

}
