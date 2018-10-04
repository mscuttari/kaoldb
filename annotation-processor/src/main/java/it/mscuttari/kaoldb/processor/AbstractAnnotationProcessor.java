package it.mscuttari.kaoldb.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

public abstract class AbstractAnnotationProcessor extends AbstractProcessor {

    /**
     * Get element utils
     *
     * @return  element utils
     */
    protected final Elements getElementUtils() {
        return processingEnv.getElementUtils();
    }


    /**
     * Get type utils
     *
     * @return  type utils
     */
    protected final Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }


    /**
     * Get filer
     *
     * @return  filer
     */
    protected final Filer getFiler() {
        return processingEnv.getFiler();
    }


    /**
     * Log error message
     *
     * @param   message     message
     */
    protected final void logError(String message) {
        logError(message, null);
    }


    /**
     * Log error message
     *
     * @param   message     message
     * @param   element     the element to use as a position hint
     */
    protected final void logError(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }


    /**
     * Log warning message
     *
     * @param   message     message
     */
    protected final void logWarning(String message) {
        logWarning(message, null);
    }


    /**
     * Log warning message
     *
     * @param   message     message
     * @param   element     the element to use as a position hint
     */
    protected final void logWarning(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

}
