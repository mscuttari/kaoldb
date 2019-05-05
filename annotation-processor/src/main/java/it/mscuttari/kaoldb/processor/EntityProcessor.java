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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class EntityProcessor extends AbstractAnnotationProcessor {

    private static final String ENTITY_SUFFIX = "_";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ClassName singlePropertyClass = ClassName.get("it.mscuttari.kaoldb.core", "SingleProperty");
        ClassName collectionPropertyClass = ClassName.get("it.mscuttari.kaoldb.core", "CollectionProperty");

        for (Element classElement : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            try {
                if (classElement.getKind() != ElementKind.CLASS) {
                    throw new ProcessorException("Element \"" + classElement.getSimpleName() + "\" should not have the @Entity annotation", classElement);
                }

                // Check the existence of a default constructor
                checkForDefaultConstructor((TypeElement) classElement);

                // Get package name
                Element enclosing = classElement;
                while (enclosing.getKind() != ElementKind.PACKAGE)
                    enclosing = enclosing.getEnclosingElement();

                PackageElement packageElement = (PackageElement) enclosing;
                String packageName = packageElement.getQualifiedName().toString();

                // Entity class
                TypeName classType = ClassName.get(classElement.asType());
                TypeSpec.Builder entityClass = TypeSpec.classBuilder(classElement.getSimpleName().toString() + ENTITY_SUFFIX);
                entityClass.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                // Get all parents fields
                Element currentElement = classElement;

                boolean primaryKeyFound = false;

                while (!ClassName.get(currentElement.asType()).equals(ClassName.OBJECT)) {
                    // Columns
                    List<? extends Element> internalElements = currentElement.getEnclosedElements();

                    for (Element internalElement : internalElements) {
                        if (internalElement.getKind() != ElementKind.FIELD) continue;

                        // Check if the field has the @Id annotation, in order to establish if
                        // the entity has at least one primary key
                        primaryKeyFound |= internalElement.getAnnotation(Id.class) != null;

                        // Skip the field if it's not annotated with @Column, @JoinColumn, @JoinColumns or @JoinTable
                        Column columnAnnotation = internalElement.getAnnotation(Column.class);
                        JoinColumn joinColumnAnnotation = internalElement.getAnnotation(JoinColumn.class);
                        JoinColumns joinColumnsAnnotation = internalElement.getAnnotation(JoinColumns.class);
                        JoinTable joinTableAnnotation = internalElement.getAnnotation(JoinTable.class);

                        // Field column annotation
                        ClassName propertyColumnAnnotation;

                        if (columnAnnotation != null) {
                            propertyColumnAnnotation = ClassName.get(Column.class);
                        } else if (joinColumnAnnotation != null) {
                            propertyColumnAnnotation = ClassName.get(JoinColumn.class);
                        } else if (joinColumnsAnnotation != null) {
                            propertyColumnAnnotation = ClassName.get(JoinColumns.class);
                        } else if (joinTableAnnotation != null) {
                            propertyColumnAnnotation = ClassName.get(JoinTable.class);
                        } else {
                            propertyColumnAnnotation = null;
                        }

                        // Field relationship annotation
                        ClassName propertyRelationshipAnnotation;

                        if (internalElement.getAnnotation(OneToOne.class) != null) {
                            propertyRelationshipAnnotation = ClassName.get(OneToOne.class);
                        } else if (internalElement.getAnnotation(OneToMany.class) != null) {
                            propertyRelationshipAnnotation = ClassName.get(OneToMany.class);
                        } else if (internalElement.getAnnotation(ManyToOne.class) != null) {
                            propertyRelationshipAnnotation = ClassName.get(ManyToOne.class);
                        } else if (internalElement.getAnnotation(ManyToMany.class) != null) {
                            propertyRelationshipAnnotation = ClassName.get(ManyToMany.class);
                        } else {
                            propertyRelationshipAnnotation = null;
                        }

                        boolean isCollectionProperty = internalElement.getAnnotation(OneToMany.class) != null ||
                                internalElement.getAnnotation(ManyToMany.class) != null;

                        // Get field name and type
                        String fieldName = internalElement.getSimpleName().toString();
                        TypeName fieldType = ClassName.get(getLinkedClass(internalElement).asType());

                        // Create the property
                        ParameterizedTypeName parameterizedField = ParameterizedTypeName.get(
                                isCollectionProperty ? collectionPropertyClass : singlePropertyClass,
                                classType, fieldType);

                        entityClass.addField(
                                FieldSpec.builder(parameterizedField, fieldName)
                                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                        .initializer(
                                                "new $T<>(" +
                                                        "$T.class, " +
                                                        "$T.class, " +
                                                        "$T.class, " +
                                                        "$S, " +
                                                        (propertyColumnAnnotation == null ? "$S, " : "$T.class, ") +
                                                        (propertyRelationshipAnnotation == null ? "$S" : "$T.class") +
                                                        ")",
                                                isCollectionProperty ? collectionPropertyClass : singlePropertyClass,
                                                classElement,
                                                currentElement,
                                                fieldType,
                                                fieldName,
                                                propertyColumnAnnotation,
                                                propertyRelationshipAnnotation
                                        )
                                        .build()
                        );
                    }

                    // Superclass
                    TypeMirror superClassTypeMirror = ((TypeElement) currentElement).getSuperclass();
                    currentElement = getElement(superClassTypeMirror);
                }

                // The entity must have at least a primary key
                if (!primaryKeyFound) {
                    throw new ProcessorException("Entity \"" + classElement.getSimpleName() + "\" doesn't have a primary key", classElement);
                }

                // Create class file
                JavaFile.builder(packageName, entityClass.build()).build().writeTo(getFiler());

            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());

            } catch (IOException e) {
                logError(e.getMessage(), classElement);
            }
        }

        return true;
    }


    /**
     * Check for default constructor existence
     *
     * @param   element     entity element
     * @throws  ProcessorException if the class doesn't have a default constructor
     */
    private static void checkForDefaultConstructor(TypeElement element) throws ProcessorException {
        // No need for a default constructor for abstract classes
        if (element.getModifiers().contains(Modifier.ABSTRACT))
            return;

        // Iterate through constructors to search for one with no arguments
        for (ExecutableElement cons : ElementFilter.constructorsIn(element.getEnclosedElements())) {
            if (cons.getParameters().isEmpty())
                return;
        }

        // Couldn't find any default constructor here
        throw new ProcessorException("Entity \"" + element.getSimpleName() + "\" doesn't have a default constructor", element);
    }

}