package it.mscuttari.kaoldb.exceptions;

public class QueryException extends KaolDBException {

    public QueryException() {
        this(null);
    }

    public QueryException(String message) {
        super(message);
    }

}
