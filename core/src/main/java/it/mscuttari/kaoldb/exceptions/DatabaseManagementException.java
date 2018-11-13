package it.mscuttari.kaoldb.exceptions;

public class DatabaseManagementException extends KaolDBException {

    public DatabaseManagementException() {

    }

    public DatabaseManagementException(String message) {
        super(message);
    }

    public DatabaseManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseManagementException(Throwable cause) {
        super(cause);
    }

}
