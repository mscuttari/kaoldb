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
 * Checks that the fields annotated with {@link Id} respect the following constraints:
 * <ul>
 *     <li>It is not annotated with {@link JoinTable}</li>
 *     <li>It is annotated with {@link Column}, {@link JoinColumn} or {@link JoinColumns}
 *     annotations</li>
 * </ul>
 */
@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Id")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class IdProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(Id.class).stream()
                .filter(field -> field.getAnnotation(JoinTable.class) != null)
                .forEach(field -> logError("Field annotated with @Id can't have @JoinTable annotation", field));

        roundEnv.getElementsAnnotatedWith(Id.class).stream()
                .filter(field ->
                        field.getAnnotation(Column.class) == null &&
                        field.getAnnotation(JoinColumn.class) == null &&
                        field.getAnnotation(JoinColumns.class) == null)
                .forEach(field -> logError("Field annotated with @Id must be annotated with @Column, @JoinColumn or @JoinColumns", field));

        return false;
    }

}
