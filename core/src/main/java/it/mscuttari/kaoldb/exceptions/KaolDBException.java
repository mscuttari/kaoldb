package it.mscuttari.kaoldb.exceptions;

public class KaolDBException extends RuntimeException {

    public KaolDBException() {

    }

    public KaolDBException(String message) {
        super(message);
    }

    public KaolDBException(String message, Throwable cause) {
        super(message, cause);
    }

    public KaolDBException(Throwable cause) {
        super(cause);
    }

}
