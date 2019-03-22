package it.mscuttari.kaoldb.processor;

import com.squareup.javapoet.ClassName;

import java.util.Collection;
import java.util.List;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.OneToMany;

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


    /**
     * Get an element given its type
     *
     * @param typeMirror    element type
     * @return element
     */
    protected final Element getElement(TypeMirror typeMirror) {
        return getTypeUtils().asElement(typeMirror);
    }


    /**
     * Get the data type of a field that implements the {@link Collection} interface
     *
     * @param field     field declared as a Collection
     *
     * @return data type (class specified in the diamond operator)
     *
     * @throws ProcessorException if the field is not a Collection
     * @throws ProcessorException if the diamond operator is not specified
     */
    protected final Element getCollectionType(Element field) throws ProcessorException {
        if (field.getKind() != ElementKind.FIELD) {
            // Security check
            throw new ProcessorException("Element " + field.getSimpleName() + " is not a field", field);
        }

        TypeMirror fieldType = field.asType();
        TypeMirror collectionType = getTypeUtils().erasure(getElementUtils().getTypeElement("java.util.Collection").asType());

        // Check that the field is declared as a Collection
        if (!getTypeUtils().erasure(fieldType).equals(collectionType))
            throw new ProcessorException("Field is not a Collection", field);

        // Check the presence of the diamond operator
        List<? extends TypeMirror> collectionInterfaceArguments = ((DeclaredType) fieldType).getTypeArguments();

        if (collectionInterfaceArguments.size() == 0)
            throw new ProcessorException("Collection data type not specified (no diamond operator found)", field);

        return getElement(collectionInterfaceArguments.get(0));
    }


    /**
     * Get the class linked to a relationship
     *
     * Examples:
     *  -   @ManyToOne
     *      Person person -> linked class = Person
     *  -   @ManyToMany
     *      Collection<Ticket> tickets -> linked class = Ticket
     *
     * @param field     field corresponding to the relationship
     * @return linked class element
     */
    protected final Element getLinkedClass(Element field) throws ProcessorException {
        if (field.getKind() != ElementKind.FIELD) {
            // Security check
            throw new ProcessorException("Element " + field.getSimpleName() + " is not a field", field);
        }

        OneToMany oneToManyAnnotation = field.getAnnotation(OneToMany.class);
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        if (oneToManyAnnotation != null || manyToManyAnnotation != null)
            return getCollectionType(field);

        return getElement(field.asType());
    }


    /**
     * Get the element of a class (and eventually its superclasses) that is generating a column
     * with a certain name.
     *
     * @param clazz             class to search in
     * @param includeParents    whether the search should be extended to the class parents
     * @param columnName        column name to be searched
     *
     * @return element that generates the column
     * @throws ProcessorException if the column can't be found
     */
    protected final Element getColumnElement(Element clazz, boolean includeParents, String columnName) throws ProcessorException {
        if (clazz.getKind() != ElementKind.CLASS) {
            // Security check
            throw new ProcessorException("Element " + clazz.getSimpleName() + " is not a class", clazz);
        }

        for (Element element : clazz.getEnclosedElements()) {
            if (element.getKind() != ElementKind.FIELD) {
                // Only the fields are currently allowed to generate columns
                continue;
            }

            // Check if the columns is generated from a @Column annotation
            Column columnAnnotation = element.getAnnotation(Column.class);

            if (columnAnnotation != null && columnAnnotation.name().equals(columnName))
                return element;

            // Check if the columns is generated from a @JoinColumn annotation
            JoinColumn joinColumnAnnotation = element.getAnnotation(JoinColumn.class);

            if (joinColumnAnnotation != null && joinColumnAnnotation.name().equals(columnName))
                return element;

            // Check if the columns is generated from a @JoinColumns annotation
            JoinColumns joinColumnsAnnotation = element.getAnnotation(JoinColumns.class);

            if (joinColumnsAnnotation != null) {
                for (JoinColumn joinColumn : joinColumnsAnnotation.value()) {
                    if (joinColumn.name().equals(columnName))
                        return element;
                }
            }
        }

        if (includeParents) {
            // Go up in class hierarchy
            TypeMirror parentTypeMirror = ((TypeElement) clazz).getSuperclass();
            Element parent = ((DeclaredType) parentTypeMirror).asElement();

            if (!ClassName.get(parent.asType()).equals(ClassName.OBJECT))
                return getColumnElement(parent, includeParents, columnName);
        }

        throw new ProcessorException("Column " + columnName + " not found in class " + clazz.getSimpleName(), clazz);
    }

}
