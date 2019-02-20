package it.mscuttari.kaoldb.exceptions;

public class DumpException extends KaolDBException {

    public DumpException() {

    }

    public DumpException(String message) {
        super(message);
    }

    public DumpException(String message, Throwable cause) {
        super(message, cause);
    }

    public DumpException(Throwable cause) {
        super(cause);
    }

}
