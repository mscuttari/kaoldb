package it.mscuttari.kaoldb.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
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
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;

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

        ClassName propertyClass = ClassName.get("it.mscuttari.kaoldb.core", "Property");

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
                TypeName classType = ClassName.get(classElement.asType());
                TypeSpec.Builder entityClass = TypeSpec.classBuilder(classElement.getSimpleName().toString() + ENTITY_SUFFIX);
                Set<Modifier> classModifiers = classElement.getModifiers();
                entityClass.addModifiers(classModifiers.toArray(new Modifier[classModifiers.size()]));

                // Superclass
                TypeElement typeClassElement = (TypeElement)classElement;
                TypeMirror parent = typeClassElement.getSuperclass();

                if (!ClassName.get(parent).equals(ClassName.OBJECT)) {
                    entityClass.superclass(ClassName.get(packageName, ClassName.get(parent).toString() + ENTITY_SUFFIX));
                }

                // Columns
                List<? extends Element> internalElements = classElement.getEnclosedElements();

                for (Element internalElement : internalElements) {
                    if (internalElement.getKind() != ElementKind.FIELD) continue;

                    // Skip the field if it's not annotated with @Column, @JoinColumn, @JoinColumns or @JoinTable
                    Column columnAnnotation = internalElement.getAnnotation(Column.class);
                    JoinColumn joinColumnAnnotation = internalElement.getAnnotation(JoinColumn.class);
                    JoinColumns joinColumnsAnnotation = internalElement.getAnnotation(JoinColumns.class);
                    JoinTable joinTableAnnotation = internalElement.getAnnotation(JoinTable.class);

                    if (columnAnnotation == null && joinColumnAnnotation == null && joinColumnsAnnotation == null && joinTableAnnotation == null)
                        continue;

                    // Create property
                    String fieldName = internalElement.getSimpleName().toString();
                    TypeName fieldType = ClassName.get(internalElement.asType());
                    ParameterizedTypeName parameterizedField = ParameterizedTypeName.get(propertyClass, classType, fieldType);

                    entityClass.addField(
                            FieldSpec.builder(parameterizedField, fieldName)
                                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                    .initializer("new $T<>($L.class, $L.class, $S);", propertyClass, classElement.getSimpleName().toString(), fieldType, fieldName)
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