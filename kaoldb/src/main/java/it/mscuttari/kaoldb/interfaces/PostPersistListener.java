package it.mscuttari.kaoldb.interfaces;

public interface PostPersistListener<M> {

    /**
     * Method called by the entity manager after persisting an object
     *
     * @param   obj     persisted object
     */
    void postPersist(M obj);

}
