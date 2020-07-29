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

import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

/**
 * Analyze all the fields with {@link Column}, {@link JoinColumn}, {@link JoinColumns} or
 * {@link JoinTable} annotations and check if the constraints are respected.
 *
 * <p>
 * {@link Column} constraints: see {@link #checkColumn(Element)}<br>
 * {@link JoinColumn} constraints: see {@link #checkJoinColumn(Element)}<br>
 * {@link JoinColumns} constraints: see {@link #checkJoinColumns(Element)}<br>
 * {@link JoinTable} constraints: see {@link #checkJoinTable(Element)}
 * </p>
 */
@SupportedAnnotationTypes({
        "it.mscuttari.kaoldb.annotations.Column",
        "it.mscuttari.kaoldb.annotations.JoinColumn",
        "it.mscuttari.kaoldb.annotations.JoinColumns",
        "it.mscuttari.kaoldb.annotations.JoinTable"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class ColumnProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        try {
            for (Element field : roundEnv.getElementsAnnotatedWith(Column.class)) {
                checkColumn(field);
            }

            for (Element field : roundEnv.getElementsAnnotatedWith(JoinColumn.class)) {
                checkJoinColumn(field);
            }

            for (Element field : roundEnv.getElementsAnnotatedWith(JoinColumns.class)) {
                checkJoinColumns(field);
            }

            for (Element field : roundEnv.getElementsAnnotatedWith(JoinTable.class)) {
                checkJoinTable(field);
            }

        } catch (ProcessorException e) {
            logError(e.getMessage(), e.getElement());
        }

        return true;
    }


    /**
     * Check field annotated with {@link Column} annotation.
     *
     * <p>
     * It ensures the following constraints are respected:
     * <ul>
     *     <li>The field doesn't have more than one annotation between {@link Column},
     *     {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}</li>
     *     <li>The field doesn't have {@link OneToOne}, {@link ManyToOne} or {@link ManyToMany}
     *     annotations</li>
     * </ul>
     * </p>
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkColumn(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check relationship absence
        if (field.getAnnotation(OneToOne.class) != null) {
            throw new ProcessorException("Field annotated with @Column can't have @OneToOne annotation", field);
        }

        if (field.getAnnotation(ManyToOne.class) != null) {
            throw new ProcessorException("Field annotated with @Column can't have @ManyToOne annotation", field);
        }

        if (field.getAnnotation(ManyToMany.class) != null) {
            throw new ProcessorException("Field annotated with @Column can't have @ManyToMany annotation", field);
        }
    }


    /**
     * Check field annotated with {@link JoinColumn} annotation.
     *
     * <p>
     * It ensures the following constraints are respected:
     * <ul>
     *     <li>The field doesn't have more than one annotation between {@link Column},
     *     {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}</li>
     *     <li>The field is annotated with {@link OneToOne}, or {@link ManyToOne} annotations</li>
     *     <li>The field is not annotated with {@link ManyToMany} annotation</li>
     *     <li>The {@link JoinColumn#referencedColumnName()} column exists</li>
     * </ul>
     * </p>
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkJoinColumn(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check relationship
        OneToOne oneToOneAnnotation     = field.getAnnotation(OneToOne.class);
        ManyToOne manyToOneAnnotation   = field.getAnnotation(ManyToOne.class);

        if (oneToOneAnnotation == null && manyToOneAnnotation == null) {
            throw new ProcessorException("Field annotated with @JoinColumn must also be annotated with @OneToOne or @ManyToOne", field);
        }

        if (field.getAnnotation(ManyToMany.class) != null) {
            throw new ProcessorException("Field annotated with @JoinColumn can't have @ManyToMany annotation", field);
        }

        // Check the referenced column
        JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
        Element linkedClass = getLinkedClass(field);

        try {
            getColumnElement(linkedClass, true, joinColumnAnnotation.referencedColumnName());

        } catch (ProcessorException e) {
            throw new ProcessorException(e.getMessage(), field);
        }
    }


    /**
     * Check field annotated with {@link JoinColumns} annotation.
     *
     * <p>
     * It ensures the following constraints are respected:
     * <ul>
     *     <li>The field doesn't have more than one annotation between {@link Column},
     *     {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}</li>
     *     <li>The field is annotated with {@link OneToOne} or {@link ManyToOne} annotations</li>
     *     <li>The field is not annotated with {@link ManyToMany} annotation</li>
     *     <li>The {@link JoinColumn#referencedColumnName()} of the join columns contained in
     *     {@link JoinColumns#value()} exist</li>
     * </ul>
     * </p>
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkJoinColumns(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check relationship
        OneToOne oneToOneAnnotation     = field.getAnnotation(OneToOne.class);
        ManyToOne manyToOneAnnotation   = field.getAnnotation(ManyToOne.class);

        if (oneToOneAnnotation == null && manyToOneAnnotation == null) {
            throw new ProcessorException("Field annotated with @JoinColumns must also be annotated with @OneToOne or @ManyToOne", field);
        }

        if (field.getAnnotation(ManyToMany.class) != null) {
            throw new ProcessorException("Field annotated with @JoinColumns can't have @ManyToMany annotation", field);
        }

        // Check referenced columns
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        Element linkedClass = getLinkedClass(field);

        try {
            for (JoinColumn joinColumnAnnotation : joinColumnsAnnotation.value()) {
                getColumnElement(linkedClass, true, joinColumnAnnotation.referencedColumnName());
            }

        } catch (ProcessorException e) {
            throw new ProcessorException(e.getMessage(), field);
        }
    }


    /**
     * Check field annotated with {@link JoinTable} annotation.
     *
     * <p>
     * It ensures the following constraints are respected:
     * <ul>
     *     <li>The field doesn't have more than one annotation between {@link Column},
     *     {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}</li>
     *     <li>The field is annotated with {@link OneToOne}, {@link ManyToOne} or
     *     {@link ManyToMany} annotations</li>
     *     <li>The {@link JoinTable#joinClass()} is the same of the declaring class</li>
     *     <li>The {@link JoinTable#inverseJoinClass()} is the same of the field type (in case of
     *     {@link OneToOne} and {@link ManyToOne}) or the same of Collection elements type (in
     *     case of {@link OneToMany} and {@link ManyToMany})</li>
     *     <li>The {@link JoinColumn#referencedColumnName()} of the direct and inverse join
     *     column exist</li>
     * </ul>
     * </p>
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkJoinTable(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check relationship
        OneToOne oneToOneAnnotation     = field.getAnnotation(OneToOne.class);
        ManyToOne manyToOneAnnotation   = field.getAnnotation(ManyToOne.class);
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        if (oneToOneAnnotation == null && manyToOneAnnotation == null && manyToManyAnnotation == null) {
            throw new ProcessorException("Field annotated with @JoinColumn must also be annotated with @OneToOne, @ManyToOne or @ManyToMany", field);
        }

        // Check the joinClass and the inverseJoinClass values
        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        // Direct join class
        TypeMirror joinClass = null;

        try {
            joinTableAnnotation.joinClass();
        } catch (MirroredTypeException e) {
            joinClass = e.getTypeMirror();
        }

        TypeMirror enclosingClass = field.getEnclosingElement().asType();

        if (!enclosingClass.equals(joinClass)) {
            throw new ProcessorException("Invalid joinClass: expected " + enclosingClass + ", found " + joinClass, field);
        }

        // Inverse join class
        TypeMirror inverseJoinClass = null;

        try {
            joinTableAnnotation.inverseJoinClass();
        } catch (MirroredTypeException e) {
            inverseJoinClass = e.getTypeMirror();
        }

        Element linkedClass = getLinkedClass(field);

        if (!linkedClass.asType().equals(inverseJoinClass)) {
            throw new ProcessorException("Invalid inverseJoinClass: expected " + linkedClass + ", found " + inverseJoinClass, field);
        }

        // Check columns references
        try {
            for (JoinColumn directJoinColumn : joinTableAnnotation.joinColumns()) {
                getColumnElement(field.getEnclosingElement(), true, directJoinColumn.referencedColumnName());
            }

            for (JoinColumn inverseJoinColumn : joinTableAnnotation.inverseJoinColumns()) {
                getColumnElement(linkedClass, true, inverseJoinColumn.referencedColumnName());
            }

        } catch (ProcessorException e) {
            throw new ProcessorException(e.getMessage(), field);
        }
    }


    /**
     * Check if the field has only one between {@link Column}, {@link JoinColumn},
     * {@link JoinColumns} and {@link JoinTable} annotations.
     *
     * @param   field       field to be checked
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkAnnotationCount(Element field) throws ProcessorException{
        Column columnAnnotation           = field.getAnnotation(Column.class);
        JoinColumn joinColumnAnnotation   = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation     = field.getAnnotation(JoinTable.class);

        int annotationCount = 0;

        if (columnAnnotation      != null) annotationCount++;
        if (joinColumnAnnotation  != null) annotationCount++;
        if (joinColumnsAnnotation != null) annotationCount++;
        if (joinTableAnnotation   != null) annotationCount++;

        if (annotationCount > 1) {
            throw new ProcessorException("Only one annotation between @Column, @JoinColumn, @JoinColumns and @JoinTable is allowed", field);
        }
    }

}