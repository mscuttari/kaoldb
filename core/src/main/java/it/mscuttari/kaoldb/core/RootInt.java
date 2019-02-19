package it.mscuttari.kaoldb.core;

import java.util.Map;

import it.mscuttari.kaoldb.interfaces.Root;

interface RootInt<X> extends Root<X> {

    /**
     * Get the entity class the root is linked to
     *
     * @return entity class
     */
    Class<X> getEntityClass();


    /**
     * Get the alias used in the query for the current root
     *
     * @return alias
     */
    String getAlias();


    /**
     * Get a map between the aliases and the roots
     *
     * @return map
     */
    Map<String, Root<?>> getRootsMap();

}
