package it.mscuttari.kaoldb.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;

@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class EntityProcessor extends AbstractProcessor {

    private static final String ENTITY_SUFFIX = "_";

    private Filer filer;
    private Messager messager;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        ClassName propertyClass = ClassName.get("it.mscuttari.kaoldb.query", "Property");

        for (Element classElement : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            if (classElement.getKind() != ElementKind.CLASS)
                messager.printMessage(Diagnostic.Kind.ERROR, "The element " + classElement.getSimpleName().toString() + " should not have @Entity annotation");

            // Get package name
            Element enclosing = classElement;
            while (enclosing.getKind() != ElementKind.PACKAGE)
                enclosing = enclosing.getEnclosingElement();

            PackageElement packageElement = (PackageElement)enclosing;
            String packageName = packageElement.getQualifiedName().toString();

            try {
                // Entity class
                TypeSpec.Builder entityClass = TypeSpec.classBuilder(classElement.getSimpleName().toString() + ENTITY_SUFFIX);
                Set<Modifier> classModifiers = classElement.getModifiers();
                entityClass.addModifiers(classModifiers.toArray(new Modifier[classModifiers.size()]));

                // Columns
                List<? extends Element> internalElements = classElement.getEnclosedElements();

                for (Element internalElement : internalElements) {
                    if (internalElement.getKind() != ElementKind.FIELD) continue;
                    Column columnAnnotation = internalElement.getAnnotation(Column.class);
                    if (columnAnnotation == null) continue;

                    String fieldName = internalElement.getSimpleName().toString();

                    entityClass.addField(
                            FieldSpec.builder(propertyClass, fieldName)
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("new $T($L.class, $S)", propertyClass, classElement.getSimpleName().toString(), columnAnnotation.name())
                                    .build()
                    );
                }

                // Create class file
                JavaFile.builder(packageName, entityClass.build()).build().writeTo(filer);

            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }

        return true;
    }

}