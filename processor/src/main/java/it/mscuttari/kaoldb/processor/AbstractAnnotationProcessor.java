package it.mscuttari.kaoldb.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public abstract class AbstractAnnotationProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }


    /**
     * Get filer
     *
     * @return  filer
     */
    protected Filer getFiler() {
        return filer;
    }


    /**
     * Log error message
     *
     * @param   message     message
     */
    protected void logError(String message) {
        logError(message, null);
    }


    /**
     * Log error message
     *
     * @param   message     message
     * @param   element     the element to use as a position hint
     */
    protected void logError(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }


    /**
     * Log warning message
     *
     * @param   message     message
     */
    protected void logWarning(String message) {
        logWarning(message, null);
    }


    /**
     * Log warning message
     *
     * @param   message     message
     * @param   element     the element to use as a position hint
     */
    protected void logWarning(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

}
