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