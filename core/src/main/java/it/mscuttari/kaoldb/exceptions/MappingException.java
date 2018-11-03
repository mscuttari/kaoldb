package it.mscuttari.kaoldb.exceptions;

public class MappingException extends KaolDBException {

    public MappingException() {

    }

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MappingException(Throwable cause) {
        super(cause);
    }

}
