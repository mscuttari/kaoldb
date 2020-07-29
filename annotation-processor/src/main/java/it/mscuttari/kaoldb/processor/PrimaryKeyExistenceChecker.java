package it.mscuttari.kaoldb.processor;

import com.squareup.javapoet.ClassName;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.Id;

/**
 * Check if all the entities have at least one primary key.
 */
@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PrimaryKeyExistenceChecker extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(Entity.class).stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .filter(element -> {
                    boolean hasPrimaryKey = false;
                    Element parent = element;

                    while (!hasPrimaryKey && !ClassName.get(parent.asType()).equals(ClassName.OBJECT)) {
                        if (parent.getAnnotation(Entity.class) != null) {
                            List<? extends Element> internalElements = parent.getEnclosedElements();

                            hasPrimaryKey = internalElements.stream()
                                    .filter(field -> field.getKind() == ElementKind.FIELD)
                                    .anyMatch(field -> field.getAnnotation(Id.class) != null);
                        }

                        parent = getSuperclass(parent);
                    }

                    return !hasPrimaryKey;
                })
                .forEach(entity -> logError("Entity doesn't have a primary key", entity));

        return false;
    }

}
