package it.mscuttari.kaoldb.exceptions;

public class DatabaseManagementException extends KaolDBException {

    public DatabaseManagementException() {
        this(null);
    }

    public DatabaseManagementException(String message) {
        super(message);
    }

}
