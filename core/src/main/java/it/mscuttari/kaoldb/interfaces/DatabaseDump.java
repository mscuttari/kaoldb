package it.mscuttari.kaoldb.interfaces;

import java.util.Collection;

import it.mscuttari.kaoldb.exceptions.DumpException;

public interface DatabaseDump {

    /**
     * Get all table dumps
     *
     * @return table dumps
     */
    Collection<TableDump> getTables();


    /**
     * Get specific table dump
     *
     * @param tableName     table name
     * @return table dump
     * @throws DumpException if the table doesn't exist in the database
     */
    TableDump getTable(String tableName) throws DumpException;

}
