package it.mscuttari.kaoldb.exceptions;

public class PojoException extends KaolDBException {

    public PojoException() {

    }

    public PojoException(String message) {
        super(message);
    }

    public PojoException(String message, Throwable cause) {
        super(message, cause);
    }

    public PojoException(Throwable cause) {
        super(cause);
    }

}
