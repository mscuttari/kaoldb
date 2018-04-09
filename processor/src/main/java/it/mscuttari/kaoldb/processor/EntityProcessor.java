package it.mscuttari.kaoldb.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import it.mscuttari.kaoldb.annotations.Entity;

@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class EntityProcessor extends AbstractProcessor {

    private static final String ENTITY_SUFFIX = "_";

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private Map<String, Element> classes;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        classes = new HashMap<>();
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Find all entities
        for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            if (element.getKind() != ElementKind.CLASS)
                messager.printMessage(Diagnostic.Kind.ERROR, "The element " + element.getSimpleName().toString() + " should not have @Entity annotation");

            TypeElement typeElement = (TypeElement) element;
            classes.put(typeElement.getSimpleName().toString(), element);
        }

        for (String className : classes.keySet()) {
            Element classElement = classes.get(className);

            // Get package name
            Element enclosing = classElement;
            while (enclosing.getKind() != ElementKind.PACKAGE)
                enclosing = enclosing.getEnclosingElement();

            PackageElement packageElement = (PackageElement)enclosing;
            String packageName = packageElement.getSimpleName().toString();

            try {
                // Entity class
                TypeSpec.Builder entityClass = TypeSpec.classBuilder(classElement.getSimpleName().toString() + ENTITY_SUFFIX);
                Set<Modifier> classModifiers = classElement.getModifiers();
                entityClass.addModifiers(classModifiers.toArray(new Modifier[classModifiers.size()]));

                MethodSpec main = MethodSpec.methodBuilder("main")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(void.class)
                        .addParameter(String[].class, "args")
                        .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
                        .build();

                entityClass.addMethod(main);


                // Create class file
                JavaFile.builder(packageName, entityClass.build()).build().writeTo(filer);

            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }


            //  Generate a class
            /*
            TypeSpec.Builder navigatorClass = TypeSpec
                    .classBuilder("Navigator")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            for (Map.Entry<String, String> element : activitiesWithPackage.entrySet()) {
                String activityName = element.getKey();
                String packageName = element.getValue();
                ClassName activityClass = ClassName.get(packageName, activityName);
                MethodSpec intentMethod = MethodSpec
                        .methodBuilder(METHOD_PREFIX + activityName)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(classIntent)
                        .addParameter(classContext, "context")
                        .addStatement("return new $T($L, $L)", classIntent, "context", activityClass + ".class")
                        .build();
                navigatorClass.addMethod(intentMethod);
            }


            // Write generated class to a file
            JavaFile.builder("com.annotationsample", navigatorClass.build()).build().writeTo(filer);


        } catch (IOException e) {
            e.printStackTrace();
        }*/

        return true;
    }

}