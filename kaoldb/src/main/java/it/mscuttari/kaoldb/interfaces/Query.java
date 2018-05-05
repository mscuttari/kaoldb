package it.mscuttari.kaoldb.interfaces;

import java.util.List;

public interface Query<M> {

    List<M> getResultList();
    M getSingleResult();

}
