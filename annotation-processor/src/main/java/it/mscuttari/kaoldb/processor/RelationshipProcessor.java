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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

/**
 * Analyze all the fields with {@link OneToOne}, {@link OneToMany}, {@link ManyToOne} or
 * {@link ManyToMany} annotations and check if the constraints are respected.
 *
 * {@link OneToOne} constraints: see {@link #checkOneToOneRelationship(Element)}
 * {@link OneToMany} constraints: see {@link #checkOneToManyRelationship(Element)}
 * {@link ManyToOne} constraints: see {@link #checkManyToOneRelationship(Element)}
 * {@link ManyToMany} constraints: see {@link #checkManyToManyRelationship(Element)}
 */
@SupportedAnnotationTypes({
        "it.mscuttari.kaoldb.annotations.OneToOne",
        "it.mscuttari.kaoldb.annotations.OneToMany",
        "it.mscuttari.kaoldb.annotations.ManyToOne",
        "it.mscuttari.kaoldb.annotations.ManyToMany"
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class RelationshipProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element field : roundEnv.getElementsAnnotatedWith(OneToOne.class)) {
            try {
                checkOneToOneRelationship(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(OneToMany.class)) {
            try {
                checkOneToManyRelationship(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(ManyToOne.class)) {
            try {
                checkManyToOneRelationship(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(ManyToMany.class)) {
            try {
                checkManyToManyRelationship(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        return true;
    }


    /**
     * Check field annotated with {@link OneToOne} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   In case of owning side, the field is also annotated with {@link JoinColumn},
     *      {@link JoinColumns} or {@link JoinTable}.
     *  -   In case of non-owning side, the field is not annotated with {@link JoinColumn},
     *      {@link JoinColumns} or {@link JoinTable}.
     *  -   If specified, the {@link OneToOne#mappedBy()} field exists, is of correct type, is
     *      annotated {@link OneToOne} and its {@link OneToOne#mappedBy()} field is empty.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkOneToOneRelationship(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);

        JoinColumn joinColumnAnnotation   = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation     = field.getAnnotation(JoinTable.class);

        if (oneToOneAnnotation.mappedBy().isEmpty()) {
            // The owning side must be annotated with @JoinColumn, @JoinColumns or @JoinTable
            if (joinColumnAnnotation == null && joinColumnsAnnotation == null && joinTableAnnotation == null)
                throw new ProcessorException("@OneToOne relationship doesn't have @JoinColumn, @JoinColumns or @JoinTable annotation", field);

        } else {
            // The non-owning side must not be annotated with @JoinColumn, @JoinColumns or @JoinTable
            if (joinColumnAnnotation != null || joinColumnsAnnotation != null || joinTableAnnotation != null)
                throw new ProcessorException("The non-owning side of a @OneToOne relationship can't have @JoinColumn, @JoinColumns or @JoinTable annotation", field);

            // Get the mapping field
            Element linkedClass = getLinkedClass(field);
            Element linkedField;

            try {
                linkedField = getClassField(linkedClass, oneToOneAnnotation.mappedBy());
            } catch (ProcessorException e) {
                throw new ProcessorException(e.getMessage(), field);
            }

            // The mapping field must be of a compatible type
            if (!getTypeUtils().isAssignable(field.getEnclosingElement().asType(), linkedField.asType()))
                throw new ProcessorException("Field \"" + oneToOneAnnotation.mappedBy() + "\" must be of type \"" + field.getEnclosingElement().getSimpleName() + "\"", linkedField);

            // The mapping field must be annotated with @OneToOne and its mappedBy value must be empty
            OneToOne linkedOneToOneAnnotation = linkedField.getAnnotation(OneToOne.class);

            if (linkedOneToOneAnnotation == null)
                throw new ProcessorException("Field \"" + oneToOneAnnotation.mappedBy() + "\" must be annotated with @OneToOne", linkedField);

            if (!linkedOneToOneAnnotation.mappedBy().isEmpty())
                throw new ProcessorException("Only one side of the @OneToOne relationship can be the owning one", field);
        }
    }


    /**
     * Check field annotated with {@link OneToMany} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field doesn't have {@link Column}, {@link JoinColumn}, {@link JoinColumns} or
     *      {@link JoinTable} annotations.
     *  -   The field class implements the {@link Collection} interface.
     *  -   The {@link OneToMany#mappedBy()} field exists, is of correct type and is annotated
     *      with {@link ManyToOne}.
     *
     * If the field class implements the {@link Collection} interface but is not declared as a
     * {@link Collection}, {@link List} or {@link Set}, a warning specifying lazy loading
     * disabling is raised.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkOneToManyRelationship(Element field) throws ProcessorException {
        // Check absence of @OneToOne, @ManyToOne and @ManyToMany annotations
        checkAnnotationCount(field);

        // Check absence of @Column, @JoinColumn, @JoinColumns and @JoinTable annotations
        Column columnAnnotation           = field.getAnnotation(Column.class);
        JoinColumn joinColumnAnnotation   = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation     = field.getAnnotation(JoinTable.class);

        if (columnAnnotation != null || joinColumnAnnotation != null || joinColumnsAnnotation != null || joinTableAnnotation != null)
            throw new ProcessorException("@OneToMany can't coexist with @Column, @JoinColumn, @JoinColumns or @JoinTable", field);

        // Check the linked field
        Element linkedClass = getLinkedClass(field);

        OneToMany oneToManyAnnotation = field.getAnnotation(OneToMany.class);
        Element linkedField;

        try {
            linkedField = getClassField(linkedClass, oneToManyAnnotation.mappedBy());
        } catch (ProcessorException e) {
            throw new ProcessorException(e.getMessage(), field);
        }

        if (!getTypeUtils().isAssignable(field.getEnclosingElement().asType(), linkedField.asType()))
            throw new ProcessorException("Field \"" + oneToManyAnnotation.mappedBy() + "\" must be of type \"" + field.getEnclosingElement().getSimpleName() + "\"", linkedField);

        ManyToOne manyToOneAnnotation = linkedField.getAnnotation(ManyToOne.class);
        if (manyToOneAnnotation == null)
            throw new ProcessorException("Field \"" + oneToManyAnnotation.mappedBy() + "\" must have @ManyToOne annotation", linkedField);

        if (!isLazyLoadingAllowed(field)) {
            // Lazy load disabled warning
            logWarning("Field \"" + field.getSimpleName() + "\" is not declared as Collection, List or Set. Lazy loading is disabled for this field", field);
        }
    }


    /**
     * Check field annotated with {@link ManyToOne} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field is annotated with {@link JoinColumn}, {@link JoinColumns} or {@link JoinTable}.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkManyToOneRelationship(Element field) throws ProcessorException {
        // Check absence of @OneToOne, @OneToMany and @ManyToMany annotations
        checkAnnotationCount(field);

        // Check presence of @JoinColumn, @JoinColumns or @JoinTable
        JoinColumn joinColumnAnnotation   = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation     = field.getAnnotation(JoinTable.class);

        if (joinColumnAnnotation == null && joinColumnsAnnotation == null && joinTableAnnotation == null)
            throw new ProcessorException("@ManyToOne relationship doesn't have @JoinColumn, @JoinColumns or @JoinTable annotation", field);
    }


    /**
     * Check field annotated with {@link ManyToMany} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   In case of owning side, the field is annotated with {@link JoinTable}.
     *  -   The field class implements the {@link Collection} interface.
     *  -   The {@link OneToMany#mappedBy()} field, if specified, exists, is a {@link Collection}
     *      of compatible type and is annotated with a {@link ManyToMany} annotation denoted by
     *      having an empty {@link OneToMany#mappedBy()} (in order to have an owning side of the
     *      relationship).
     *
     * If the field class implements the {@link Collection} interface but is not declared as a
     * {@link Collection}, {@link List} or {@link Set}, a warning specifying lazy loading
     * disabling is raised.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraint are not respected
     */
    private void checkManyToManyRelationship(Element field) throws ProcessorException {
        // Check absence of @OneToOne, @OneToMany and @ManyToOne annotations
        checkAnnotationCount(field);

        ManyToMany annotation = field.getAnnotation(ManyToMany.class);

        // Check the linked field
        Element linkedClass = getLinkedClass(field);

        if (annotation.mappedBy().isEmpty()) {
            // Owning side of the relationship ("mappedBy" field is empty).
            // The field must have the @JoinTable annotation.

            if (field.getAnnotation(JoinTable.class) == null)
                throw new ProcessorException("@ManyToMany relationship must have a @JoinTable annotation", field);

        } else {
            // Non-owning side of the relationship
            Element linkedField;

            try {
                linkedField = getClassField(linkedClass, annotation.mappedBy());
            } catch (ProcessorException e) {
                throw new ProcessorException(e.getMessage(), field);
            }

            ManyToMany linkedFieldAnnotation = linkedField.getAnnotation(ManyToMany.class);

            if (!linkedFieldAnnotation.mappedBy().isEmpty()) {
                // Check that the linked field is not market as non-owning side too
                throw new ProcessorException("Only one owning side of the relationship is allowed", linkedField);
            }

            // Check that the data type is compatible
            Element linkedCollectionType = getCollectionType(linkedField);

            if (!getTypeUtils().isAssignable(field.getEnclosingElement().asType(), linkedCollectionType.asType()))
                throw new ProcessorException("Collection type must be of type " + getTypeUtils().asElement(field.getEnclosingElement().asType()).getSimpleName(), linkedField);
        }

        if (!isLazyLoadingAllowed(field)) {
            // Lazy load disabled warning
            logWarning("Field \"" + field.getSimpleName() + "\" is not declared as Collection, List or Set. Lazy loading is disabled for this field", field);
        }
    }


    /**
     * Check if the field has only one between {@link OneToOne}, {@link OneToMany},
     * {@link ManyToOne} and {@link ManyToMany} annotations.
     *
     * @param   field       field to be checked
     * @throws  ProcessorException if the constraint is not respected
     */
    private void checkAnnotationCount(Element field) throws ProcessorException {
        OneToOne columnAnnotation       = field.getAnnotation(OneToOne.class);
        OneToMany joinColumnAnnotation  = field.getAnnotation(OneToMany.class);
        ManyToOne joinColumnsAnnotation = field.getAnnotation(ManyToOne.class);
        ManyToMany joinTableAnnotation  = field.getAnnotation(ManyToMany.class);

        int annotationCount = 0;

        if (columnAnnotation      != null) annotationCount++;
        if (joinColumnAnnotation  != null) annotationCount++;
        if (joinColumnsAnnotation != null) annotationCount++;
        if (joinTableAnnotation   != null) annotationCount++;

        if (annotationCount > 1) {
            throw new ProcessorException("Only one annotation between @OneToOne, @OneToMany, @ManyToOne and @ManyToMany is allowed", field);
        }
    }


    /**
     * Get a class field given its name
     *
     * @param clazz     class within to search for the field
     * @param fieldName field name
     *
     * @return field
     *
     * @throws ProcessorException if the class doesn't contain any fields with the specified name
     */
    private Element getClassField(Element clazz, String fieldName) throws ProcessorException {
        for (Element field : clazz.getEnclosedElements()) {
            if (field.getKind() == ElementKind.FIELD && field.getSimpleName().toString().equals(fieldName))
                return field;
        }

        throw new ProcessorException("Field \"" + fieldName + "\" not found in class \"" + clazz.getSimpleName() + "\"", clazz);
    }

}
