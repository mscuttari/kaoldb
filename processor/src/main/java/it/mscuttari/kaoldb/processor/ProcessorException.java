package it.mscuttari.kaoldb.processor;

import javax.lang.model.element.Element;

public class ProcessorException extends Exception {

    private String message;
    private Element element;


    /**
     * Constructor
     *
     * @param   message     error message
     * @param   element     element causing the error
     */
    public ProcessorException(String message, Element element) {
        this.message = message;
        this.element = element;
    }


    @Override
    public String getMessage() {
        return message;
    }


    public Element getElement() {
        return element;
    }

}
