package it.mscuttari.kaoldb.exceptions;

public class PojoException extends KaolDBException {

    public PojoException() {
        this(null);
    }

    public PojoException(String message) {
        super(message);
    }

}
