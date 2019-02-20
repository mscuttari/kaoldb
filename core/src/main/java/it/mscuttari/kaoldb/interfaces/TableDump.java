package it.mscuttari.kaoldb.interfaces;

import java.util.List;

public interface TableDump {

    /**
     * Get table name
     *
     * @return table name
     */
    String getName();


    /**
     * Get all the column names
     *
     * @return columns list
     */
    List<String> getColumns();


    /**
     * Get row dumps
     *
     * @return row dumps
     */
    List<RowDump> getRows();

}
