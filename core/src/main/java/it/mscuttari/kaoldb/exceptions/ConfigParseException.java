package it.mscuttari.kaoldb.exceptions;

public class ConfigParseException extends KaolDBException {

    public ConfigParseException() {
        this(null);
    }

    public ConfigParseException(String message) {
        super(message);
    }

}
