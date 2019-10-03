/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.processor;

import com.squareup.javapoet.ClassName;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;

public abstract class AbstractAnnotationProcessor extends AbstractProcessor {

    /**
     * Get element utils.
     *
     * @return  element utils
     */
    protected final Elements getElementUtils() {
        return processingEnv.getElementUtils();
    }


    /**
     * Get type utils.
     *
     * @return  type utils
     */
    protected final Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }


    /**
     * Get filer.
     *
     * @return  filer
     */
    protected final Filer getFiler() {
        return processingEnv.getFiler();
    }


    /**
     * Log error message.
     *
     * @param   message     message
     */
    protected final void logError(String message) {
        logError(message, null);
    }


    /**
     * Log error message.
     *
     * @param   message     message
     * @param   element     the element to use as a position hint
     */
    protected final void logError(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }


    /**
     * Log warning message.
     *
     * @param   message     message
     */
    protected final void logWarning(String message) {
        logWarning(message, null);
    }


    /**
     * Log warning message.
     *
     * @param   message     message
     * @param   element     the element to use as a position hint
     */
    protected final void logWarning(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }


    /**
     * Get an element given its type.
     *
     * @param typeMirror    element type
     * @return element
     */
    protected final Element getElement(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return getTypeUtils().boxedClass((PrimitiveType) typeMirror);
        }

        return getTypeUtils().asElement(typeMirror);
    }


    /**
     * Get the package within the element is declared
     *
     * @param element   element whose package has to be determined
     * @return package
     */
    protected final PackageElement getPackage(Element element) {
        while (element.getKind() != ElementKind.PACKAGE)
            element = element.getEnclosingElement();

        return (PackageElement) element;
    }


    /**
     * Get superclass.
     *
     * @param element   class element
     * @return superclass element
     * @throws IllegalArgumentException if the provided element is not a class
     */
    protected final Element getSuperclass(Element element) {
        if (element.getKind() != ElementKind.CLASS)
            throw new IllegalArgumentException("Element \"" + element.getSimpleName() + "\" is not a class");

        TypeMirror typeMirror = ((TypeElement) element).getSuperclass();
        return getElement(typeMirror);
    }


    /**
     * Get the data type of a field that implements the {@link Collection} interface.
     *
     * @param field     field implementing the Collection interface
     *
     * @return data type (class specified in the diamond operator of the Collection)
     *
     * @throws ProcessorException if the given element is not a field
     * @throws ProcessorException if the field is not a Collection
     * @throws ProcessorException if the diamond operator is not specified
     */
    protected final Element getCollectionType(Element field) throws ProcessorException {
        if (field.getKind() != ElementKind.FIELD) {
            // Security check
            throw new ProcessorException("Element \"" + field.getSimpleName() + "\" is not a field", field);
        }

        TypeMirror collectionType = getTypeUtils().erasure(getElementUtils().getTypeElement("java.util.Collection").asType());
        boolean isCollection = getTypeUtils().isAssignable(getTypeUtils().erasure(field.asType()), collectionType);

        if (!isCollection) {
            throw new ProcessorException("Field doesn't implement the Collection interface", field);
        }

        // Search the Collection interface
        Stack<Element> elements = new Stack<>();
        elements.push(field);

        while (!elements.isEmpty()) {
            Element element = elements.peek();

            if (getTypeUtils().erasure(element.asType()).equals(collectionType)) {
                // Check the presence of the diamond operator
                List<? extends TypeMirror> collectionInterfaceArguments = ((DeclaredType) element.asType()).getTypeArguments();

                if (collectionInterfaceArguments.size() == 0)
                    throw new ProcessorException("Collection data type not specified (no diamond operator found)", field);

                // Get the data type. It may be a generic, and in that case we need to map it back
                // to the effective class

                Element dataType = getElement(collectionInterfaceArguments.get(0));

                while (!elements.empty()) {
                    Element parent = elements.pop();

                    // Get the position of the generic in the parent class declaration
                    int parentGenericIndex = 0;
                    List<? extends TypeMirror> parentGenerics = ((DeclaredType) parent.asType()).getTypeArguments();

                    for (TypeMirror generic : parentGenerics) {
                        if (generic.toString().equals(dataType.asType().toString()))
                            break;

                        parentGenericIndex++;
                    }

                    // Generic is not propagated to child
                    if (parentGenericIndex >= parentGenerics.size() || elements.empty())
                        return dataType;

                    // Get the label used in the child for the same generic
                    Element child = elements.peek();
                    List<? extends TypeMirror> childInterfaces = getTypeUtils().directSupertypes(child.asType());

                    for (TypeMirror interf : childInterfaces) {
                        if (getTypeUtils().erasure(interf).equals(getTypeUtils().erasure(parent.asType()))) {
                            TypeMirror genericType = ((DeclaredType) interf).getTypeArguments().get(parentGenericIndex);
                            dataType = getElement(genericType);
                            break;
                        }
                    }
                }

                return dataType;
            }

            // Add the superclass and the interfaces to the stack
            List<? extends TypeMirror> superTypes = getTypeUtils().directSupertypes(element.asType());

            for (TypeMirror typeMirror : superTypes) {
                if (getTypeUtils().isAssignable(getTypeUtils().erasure(typeMirror), collectionType)) {
                    elements.add(getElement(typeMirror));
                    break;
                }
            }
        }

        throw new ProcessorException("Field doesn't implement the Collection interface", field);
    }


    /**
     * Check if a field is eligible for lazy loading.
     * <p>A field is considered eligible for lazy loading only if it is declared as a
     * {@link Collection}, a {@link List} or a {@link Set}.</p>
     *
     * @param field     field implementing the Collection interface
     * @return true if the field is eligible for lazy loading; false otherwise
     * @throws ProcessorException if the given element is not a field
     */
    protected final boolean isLazyLoadingAllowed(Element field) throws ProcessorException {
        if (field.getKind() != ElementKind.FIELD) {
            // Security check
            throw new ProcessorException("Element \"" + field.getSimpleName() + "\" is not a field", field);
        }

        // Remove the diamond operator
        TypeMirror erasure = getTypeUtils().erasure(field.asType());

        // Collection
        TypeMirror collectionType = getTypeUtils().erasure(getElementUtils().getTypeElement("java.util.Collection").asType());
        boolean isCollection = getTypeUtils().isAssignable(erasure, collectionType);
        if (isCollection) return true;

        // Type
        TypeMirror listType = getTypeUtils().erasure(getElementUtils().getTypeElement("java.util.List").asType());
        boolean isList = getTypeUtils().isAssignable(erasure, listType);
        if (isList) return true;

        // Set
        TypeMirror setType = getTypeUtils().erasure(getElementUtils().getTypeElement("java.util.Set").asType());
        boolean isSet = getTypeUtils().isAssignable(erasure, setType);
        if (isSet) return true;

        return false;
    }


    /**
     * Get the class linked to a relationship
     *
     * <p>
     * Examples:
     * <ul>
     *     <li>{@link ManyToOne}<br>
     *     Person person -> linked class = Person</li>
     *     <li>{@link ManyToMany}<br>
     *     Collection<Ticket> tickets -> linked class = Ticket</li>
     * </ul>
     * </p>
     *
     * @param field     field corresponding to the relationship
     * @return linked class element
     */
    protected final Element getLinkedClass(Element field) throws ProcessorException {
        if (field.getKind() != ElementKind.FIELD) {
            // Security check
            throw new ProcessorException("Element \"" + field.getSimpleName() + "\" is not a field", field);
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
            throw new ProcessorException("Element \"" + clazz.getSimpleName() + "\" is not a class", clazz);
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

        throw new ProcessorException("Column \"" + columnName + "\" not found in class \"" + clazz.getSimpleName() + "\"", clazz);
    }

}
