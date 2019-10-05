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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;

/**
 * Analyze the entities in order to determine if the hierarchy tree is coherent with respect to
 * discriminator columns and values.
 */
@SupportedAnnotationTypes({
        "it.mcsuttari.kaoldb.annotations.Entity",
        "it.mscuttari.kaoldb.annotations.DiscriminatorColumn",
        "it.mscuttari.kaoldb.annotations.DiscriminatorValue",
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class InheritanceProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        try {
            for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
                checkEntity(element);
            }

            for (Element element : roundEnv.getElementsAnnotatedWith(DiscriminatorColumn.class)) {
                checkDiscriminatorColumn(element);
            }

            for (Element element : roundEnv.getElementsAnnotatedWith(DiscriminatorValue.class)) {
                checkDiscriminatorValue(element);
            }

            // Check the uniqueness of each discriminator value

            Map<Element, Collection<String>> parentsMap = new HashMap<>();

            for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
                boolean parentFound;
                Element current = element;

                do {
                    current = getSuperclass(current);
                    parentFound = current.getAnnotation(Entity.class) != null;

                } while (!parentFound && !ClassName.get(current.asType()).equals(ClassName.OBJECT));

                if (parentFound) {
                    Collection<String> discriminatorValues = parentsMap.containsKey(current) ?
                            parentsMap.get(current) :
                            new HashSet<>();

                    parentsMap.put(current, discriminatorValues);

                    DiscriminatorValue discriminatorValue = element.getAnnotation(DiscriminatorValue.class);

                    if (discriminatorValue != null) {
                        if (discriminatorValues.contains(discriminatorValue.value()))
                            throw new ProcessorException("Discriminator value \"" + discriminatorValue.value() + "\" already used", element);

                        discriminatorValues.add(discriminatorValue.value());
                    }
                }
            }

        } catch (ProcessorException e) {
            logError(e.getMessage(), e.getElement());
        }

        return true;
    }


    /**
     * Check an element annotated with {@link Entity}.
     *
     * <p>
     * It ensures the following constraints are respected:
     * <ul>
     *     <li>The element is a class</li>
     *     <li>The class is annotated with {@link DiscriminatorValue} if it has a parent entity</li>
     * </ul>
     * </p>
     *
     * @param element       class element
     * @throws ProcessorException if some constraints are not respected
     */
    private void checkEntity(Element element) throws ProcessorException {
        // Security check
        if (element.getAnnotation(Entity.class) == null) {
            return;
        }

        // Check that the element is a class
        if (element.getKind() != ElementKind.CLASS) {
            throw new ProcessorException("Element \"" + element.getSimpleName() + "\" should not have @Entity annotation", element);
        }

        // Check for @DiscriminatorValue if a parent entity exists
        boolean parentFound;
        Element currentClass = element;

        do {
            currentClass = getSuperclass(currentClass);
            parentFound = currentClass.getAnnotation(Entity.class) != null;

        } while (!parentFound && !ClassName.get(currentClass.asType()).equals(ClassName.OBJECT));

        if (parentFound) {
            if (currentClass.getAnnotation(DiscriminatorColumn.class) == null) {
                throw new ProcessorException("Discriminator column not set", currentClass);
            }

            if (element.getAnnotation(DiscriminatorValue.class) == null) {
                throw new ProcessorException("Discriminator value not set", element);
            }
        }
    }


    /**
     * Check an element annotated with {@link DiscriminatorColumn}.
     *
     * <p>
     * It ensures the following constraints are respected:
     * <ul>
     *     <li>The element is an abstract class</li>
     *     <li>The element is annotated with {@link Entity}</li>
     * </ul>
     * </p>
     *
     * @param element       class element
     * @throws ProcessorException if some constraints are not respected
     */
    private void checkDiscriminatorColumn(Element element) throws ProcessorException {
        // Security check
        if (element.getAnnotation(DiscriminatorColumn.class) == null) {
            return;
        }

        // Check that the element is an abstract class
        if (element.getKind() != ElementKind.CLASS) {
            throw new ProcessorException("Element \"" + element.getSimpleName() + "\" should not have @DiscriminatorColumn annotation", element);
        }

        if (!element.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new ProcessorException("Class \"" + element.getSimpleName() + "\" should be abstract", element);
        }

        // Check that the class is an entity
        if (element.getAnnotation(Entity.class) == null) {
            logError("Class \"" + element.getSimpleName() + "\" is not an entity", element);
        }
    }


    /**
     * Check an element annotated with {@link DiscriminatorValue}.
     *
     * <p>
     * It ensures the following constraints are respected:
     * <ul>
     *     <li>The element is a class</li>
     *     <li>The element is annotated with {@link Entity}</li>
     *     <li>Among the superclasses, the first one annotated with {@link Entity} is also
     *     annotated with {@link DiscriminatorColumn} of proper type</li>
     * </ul>
     * </p>
     *
     * @param element       class element
     * @throws ProcessorException if some constraints are not respected
     */
    private void checkDiscriminatorValue(Element element) throws ProcessorException {
        DiscriminatorValue discriminatorValueAnnotation = element.getAnnotation(DiscriminatorValue.class);

        // Security check
        if (discriminatorValueAnnotation == null) {
            return;
        }

        // Check that the element is a class
        if (element.getKind() != ElementKind.CLASS) {
            throw new ProcessorException("Element \"" + element.getSimpleName() + "\" should not have @DiscriminatorValue annotation", element);
        }

        // Check that the class is an entity
        if (element.getAnnotation(Entity.class) == null) {
            logError("Class \"" + element.getSimpleName() + "\" is not an entity", element);
        }

        // Search the first parent entity and check for DiscriminatorColumn existence and compatibility

        boolean parentFound = false;
        Element currentClass = getSuperclass(element);

        while (!parentFound && !ClassName.get(currentClass.asType()).equals(ClassName.OBJECT)) {
            Entity entityAnnotation = currentClass.getAnnotation(Entity.class);
            DiscriminatorColumn discriminatorColumnAnnotation = currentClass.getAnnotation(DiscriminatorColumn.class);

            if (entityAnnotation != null) {
                if (discriminatorColumnAnnotation == null) {
                    throw new ProcessorException("Entity \"" + currentClass.getSimpleName() + "\" is not annotated with @DiscriminatorColumn", currentClass);
                }

                switch (discriminatorColumnAnnotation.discriminatorType()) {
                    case CHAR:
                    case STRING:
                        if (discriminatorValueAnnotation.value().length() < 1) {
                            throw new ProcessorException("Discriminator value can't be empty", element);
                        }

                        break;

                    case INTEGER:
                        try {
                            Integer.parseInt(discriminatorValueAnnotation.value());
                        } catch (NumberFormatException e) {
                            throw new ProcessorException("Incompatible discriminator value", element);
                        }

                        break;

                    default:
                        throw new IllegalStateException("Unknown discriminator type");
                }

                parentFound = true;
            }

            currentClass = getSuperclass(currentClass);
        }

        if (!parentFound) {
            throw new ProcessorException("No parent entity found", element);
        }
    }

}