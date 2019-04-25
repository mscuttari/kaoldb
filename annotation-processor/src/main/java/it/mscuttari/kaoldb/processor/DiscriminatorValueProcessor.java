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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import it.mscuttari.kaoldb.annotations.DiscriminatorValue;
import it.mscuttari.kaoldb.annotations.Entity;

/**
 * Checks that the {@link DiscriminatorValue} annotation is applied on a concrete entity class
 */
@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.DiscriminatorValue")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class DiscriminatorValueProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(DiscriminatorValue.class)) {
            // Check that the element is a class
            if (element.getKind() != ElementKind.CLASS) {
                logError("Element " + element.getSimpleName() + " should not have @DiscriminatorValue annotation", element);
                continue;
            }

            // Check that the class has the @Entity annotation
            Entity entityAnnotation = element.getAnnotation(Entity.class);

            if (entityAnnotation == null) {
                logError("Class " + element.getSimpleName() + " is not an entity", element);
            }
        }

        return true;
    }

}