package it.mscuttari.kaoldb.interfaces;

import it.mscuttari.kaoldb.exceptions.DumpException;

public interface RowDump {

    /**
     * Get the value of a column at this row
     *
     * @param columnName    column name
     * @param <T>           data type
     *
     * @return column value
     * @throws DumpException if the expected data type is different than the one in the database
     */
    <T> T getColumnValue(String columnName);

}
