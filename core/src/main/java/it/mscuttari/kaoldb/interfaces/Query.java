package it.mscuttari.kaoldb.interfaces;

import java.util.List;

/**
 * @param   <M>     result objects class
 */
public interface Query<M> {

    /**
     * Run the query and get the results list
     *
     * @return results list
     */
    List<M> getResultList();


    /**
     * Run the query and get the first query result object
     *
     * @return first result object
     */
    M getSingleResult();

}
