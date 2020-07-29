package it.mscuttari.kaoldb.processor;

import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import it.mscuttari.kaoldb.annotations.Entity;

/**
 * Check if the the {@link Entity} is applied only to classes.
 * In fact, a FIELD annotation can in principle be applied also to interfaces and enums.
 */
@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class EntityProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(Entity.class).stream()
                .filter(element -> element.getKind() != ElementKind.CLASS)
                .forEach(element -> logError("@Entity annotation is not allowed", element));

        return false;
    }

}
