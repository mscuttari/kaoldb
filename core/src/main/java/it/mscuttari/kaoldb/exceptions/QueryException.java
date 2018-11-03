package it.mscuttari.kaoldb.exceptions;

public class QueryException extends KaolDBException {

    public QueryException() {

    }

    public QueryException(String message) {
        super(message);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryException(Throwable cause) {
        super(cause);
    }

}
