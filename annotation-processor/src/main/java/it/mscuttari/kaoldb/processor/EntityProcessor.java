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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.DiscriminatorColumn;
import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
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

        for (Element entity : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            try {
                // A FIELD annotation can be applied also to interfaces and enums.
                // Therefore it is needed to check that is applied on a class
                if (entity.getKind() != ElementKind.CLASS) {
                    throw new ProcessorException("Element \"" + entity.getSimpleName() + "\" should not have the @Entity annotation", entity);
                }

                // Check that the inheritance is correctly used
                checkInheritance(entity);

                // Check the existence of at least one primary key
                checkPrimaryKeyExistence(entity);

                // Get package name
                PackageElement packageElement = getPackage(entity);
                String packageName = packageElement.getQualifiedName().toString();

                // Entity class
                TypeName entityClass = ClassName.get(entity.asType());
                TypeSpec.Builder generatedEntityClass = TypeSpec.classBuilder(entity.getSimpleName().toString() + ENTITY_SUFFIX);
                generatedEntityClass.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                // Get all parents fields
                Element currentElement = entity;

                while (!ClassName.get(currentElement.asType()).equals(ClassName.OBJECT)) {
                    // Columns
                    List<? extends Element> internalElements = currentElement.getEnclosedElements();

                    for (Element internalElement : internalElements) {
                        if (internalElement.getKind() != ElementKind.FIELD) continue;

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
                                entityClass, fieldType);

                        generatedEntityClass.addField(
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
                                                entity,
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
                    currentElement = getSuperclass(currentElement);
                }

                // Create class file
                JavaFile.builder(packageName, generatedEntityClass.build()).build().writeTo(getFiler());

            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());

            } catch (IOException e) {
                logError(e.getMessage(), entity);
            }
        }

        return true;
    }


    /**
     * If the specified <code>entity</code> extends another entity, check that the child
     * entity is annotated with {@link DiscriminatorValue}.
     *
     * @param entity     entity element
     * @throws ProcessorException if {@link DiscriminatorValue} or {@link DiscriminatorColumn}
     *                            are missing respectively in the child and the parent classes
     */
    private void checkInheritance(Element entity) throws ProcessorException {
        boolean hasParent = false;
        Element parent = entity;

        while (!hasParent && !ClassName.get(parent.asType()).equals(ClassName.OBJECT)) {
            TypeMirror superClassTypeMirror = ((TypeElement) parent).getSuperclass();
            parent = getElement(superClassTypeMirror);
            Entity entityAnnotation = parent.getAnnotation(Entity.class);
            hasParent = entityAnnotation != null;
        }

        if (hasParent) {
            if (entity.getAnnotation(DiscriminatorValue.class) == null) {
                throw new ProcessorException("Child entity \"" + entity.getSimpleName() + "\" doesn't have the @DiscriminatorValue annotation", entity);
            }

            if (parent.getAnnotation(DiscriminatorColumn.class) == null) {
                throw new ProcessorException("Parent entity \"" + parent.getSimpleName() + "\" doesn't have the @DiscriminatorColumn annotation", parent);
            }
        }
    }


    /**
     * Check if an entity has at least one primary key.
     *
     * @param entity    entity element
     * @throws ProcessorException if the entity or its parent entities doesn't have any field
     *                            annotated with {@link Id}
     */
    private void checkPrimaryKeyExistence(Element entity) throws ProcessorException {
        boolean hasPrimaryKey = false;
        Element parent = entity;

        while (!hasPrimaryKey && !ClassName.get(parent.asType()).equals(ClassName.OBJECT)) {
            if (parent.getAnnotation(Entity.class) != null) {
                List<? extends Element> internalElements = parent.getEnclosedElements();

                for (Element field : internalElements) {
                    if (field.getKind() != ElementKind.FIELD)
                        continue;

                    hasPrimaryKey |= field.getAnnotation(Id.class) != null;
                }
            }

            TypeMirror superClassTypeMirror = ((TypeElement) parent).getSuperclass();
            parent = getElement(superClassTypeMirror);
        }

        if (!hasPrimaryKey) {
            throw new ProcessorException("Entity \"" + entity.getSimpleName() + "\" doesn't have a primary key", entity);
        }
    }

}