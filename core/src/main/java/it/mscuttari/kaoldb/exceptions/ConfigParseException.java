package it.mscuttari.kaoldb.exceptions;

public class ConfigParseException extends KaolDBException {

    public ConfigParseException() {

    }

    public ConfigParseException(String message) {
        super(message);
    }

    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigParseException(Throwable cause) {
        super(cause);
    }

}
