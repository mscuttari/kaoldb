package it.mscuttari.kaoldb.interfaces;

import java.util.List;

public interface Query<M> {

    /**
     * Get query results list
     *
     * @return  list
     */
    List<M> getResultList();


    /**
     * Get the first query result element
     *
     * @return  fjrst result element
     */
    M getSingleResult();

}
