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

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Id;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;

/**
 * Checks that the {@link Id} annotation is applied only on fields that directly represent columns.
 */
@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Id")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class IdProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element field : roundEnv.getElementsAnnotatedWith(Id.class)) {
            try {
                checkField(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        return true;
    }


    /**
     * Check field annotated with {@link Id} annotation.
     *
     * <p>
     * It ensures the following constraints are respected:
     * <ul>
     *     <li>The field is not annotated with {@link JoinTable}</li>
     *     <li>The field is annotated with {@link Column}, {@link JoinColumn} or
     *     {@link JoinColumns} annotations</li>
     * </ul>
     * </p>
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private static void checkField(Element field) throws ProcessorException {
        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        if (joinTableAnnotation != null) {
            throw new ProcessorException("Field annotated with @Id can't have @JoinTable annotation", field);
        }

        Column columnAnnotation = field.getAnnotation(Column.class);
        JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);

        if (columnAnnotation == null && joinColumnAnnotation == null && joinColumnsAnnotation == null) {
            throw new ProcessorException("Field annotated with @Id must be annotated with @Column, @JoinColumn or @JoinColumns", field);
        }
    }

}
