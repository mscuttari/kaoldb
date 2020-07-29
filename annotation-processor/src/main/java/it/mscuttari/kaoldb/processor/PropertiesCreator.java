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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class PropertiesCreator extends AbstractAnnotationProcessor {

    private static final String ENTITY_SUFFIX = "_";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ClassName singlePropertyClass = ClassName.get("it.mscuttari.kaoldb.query", "SingleProperty");
        ClassName collectionPropertyClass = ClassName.get("it.mscuttari.kaoldb.query", "CollectionProperty");

        Collection<Element> entities = roundEnv.getElementsAnnotatedWith(Entity.class).stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .collect(Collectors.toList());

        for (Element entity : entities) {
            try {
                String packageName = getPackage(entity).getQualifiedName().toString();

                // Entity class
                TypeName entityClass = ClassName.get(entity.asType());
                TypeSpec.Builder generatedEntityClass = TypeSpec.classBuilder(entity.getSimpleName().toString() + ENTITY_SUFFIX);
                generatedEntityClass.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                // Get all parents fields
                Element currentElement = entity;

                while (!ClassName.get(currentElement.asType()).equals(ClassName.OBJECT)) {
                    List<Element> fields = currentElement.getEnclosedElements().stream()
                            .filter(element -> element.getKind() == ElementKind.FIELD)
                            .collect(Collectors.toList());

                    for (Element field : fields) {
                        ClassName columnAnnotation = getColumnAnnotation(field);
                        ClassName relationshipAnnotation = getRelationshipAnnotation(field);

                        boolean isCollectionProperty = field.getAnnotation(OneToMany.class) != null ||
                                field.getAnnotation(ManyToMany.class) != null;

                        // Get field name and type
                        String fieldName = field.getSimpleName().toString();
                        TypeName fieldType = ClassName.get(getLinkedClass(field).asType());

                        // Create the property
                        ParameterizedTypeName parameterizedField = ParameterizedTypeName.get(
                                isCollectionProperty ? collectionPropertyClass : singlePropertyClass,
                                entityClass, fieldType);

                        FieldSpec property = FieldSpec.builder(parameterizedField, fieldName)
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .initializer(
                                        "new $T<>(" +
                                                "$T.class, " +
                                                "$T.class, " +
                                                "$T.class, " +
                                                "$S, " +
                                                (columnAnnotation == null ? "$S, " : "$T.class, ") +
                                                (relationshipAnnotation == null ? "$S" : "$T.class") +
                                                ")",
                                        isCollectionProperty ? collectionPropertyClass : singlePropertyClass,
                                        entity,
                                        currentElement,
                                        fieldType,
                                        fieldName,
                                        columnAnnotation,
                                        relationshipAnnotation
                                )
                                .build();

                        generatedEntityClass.addField(property);
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

        return false;
    }

    private ClassName getColumnAnnotation(Element field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        if (columnAnnotation != null) {
            return ClassName.get(Column.class);
        } else if (joinColumnAnnotation != null) {
            return ClassName.get(JoinColumn.class);
        } else if (joinColumnsAnnotation != null) {
            return ClassName.get(JoinColumns.class);
        } else if (joinTableAnnotation != null) {
            return ClassName.get(JoinTable.class);
        } else {
            return null;
        }
    }

    private ClassName getRelationshipAnnotation(Element field) {
        if (field.getAnnotation(OneToOne.class) != null) {
            return ClassName.get(OneToOne.class);
        } else if (field.getAnnotation(OneToMany.class) != null) {
            return ClassName.get(OneToMany.class);
        } else if (field.getAnnotation(ManyToOne.class) != null) {
            return ClassName.get(ManyToOne.class);
        } else if (field.getAnnotation(ManyToMany.class) != null) {
            return ClassName.get(ManyToMany.class);
        } else {
            return null;
        }
    }

}