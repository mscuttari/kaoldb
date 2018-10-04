package it.mscuttari.kaoldb.exceptions;

public class InvalidConfigException extends KaolDBException {

    public InvalidConfigException() {
        this(null);
    }

    public InvalidConfigException(String message) {
        super(message);
    }

}
