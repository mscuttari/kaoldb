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

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.Entity;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;

@SupportedAnnotationTypes("it.mscuttari.kaoldb.annotations.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class EntityProcessor extends AbstractAnnotationProcessor {

    private static final String ENTITY_SUFFIX = "_";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        ClassName propertyClass = ClassName.get("it.mscuttari.kaoldb.core", "Property");

        for (Element classElement : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            if (classElement.getKind() != ElementKind.CLASS) {
                logError("Element " + classElement.getSimpleName() + " should not have @Entity annotation", classElement);
                continue;
            }

            // Check the existence of a default constructor
            try {
                checkForDefaultConstructor((TypeElement) classElement);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
                continue;
            }

            // Get package name
            Element enclosing = classElement;
            while (enclosing.getKind() != ElementKind.PACKAGE)
                enclosing = enclosing.getEnclosingElement();

            PackageElement packageElement = (PackageElement) enclosing;
            String packageName = packageElement.getQualifiedName().toString();

            try {
                // Entity class
                TypeName classType = ClassName.get(classElement.asType());
                TypeSpec.Builder entityClass = TypeSpec.classBuilder(classElement.getSimpleName().toString() + ENTITY_SUFFIX);
                entityClass.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                // Get all parents fields
                Element currentElement = classElement;

                while (!ClassName.get(currentElement.asType()).equals(ClassName.OBJECT)) {
                    // Columns
                    List<? extends Element> internalElements = currentElement.getEnclosedElements();

                    for (Element internalElement : internalElements) {
                        if (internalElement.getKind() != ElementKind.FIELD) continue;

                        // Skip the field if it's not annotated with @Column, @JoinColumn, @JoinColumns or @JoinTable
                        Column columnAnnotation           = internalElement.getAnnotation(Column.class);
                        JoinColumn joinColumnAnnotation   = internalElement.getAnnotation(JoinColumn.class);
                        JoinColumns joinColumnsAnnotation = internalElement.getAnnotation(JoinColumns.class);
                        JoinTable joinTableAnnotation     = internalElement.getAnnotation(JoinTable.class);

                        if (columnAnnotation == null && joinColumnAnnotation == null && joinColumnsAnnotation == null && joinTableAnnotation == null)
                            continue;

                        // Get field name and type
                        String fieldName = internalElement.getSimpleName().toString();
                        TypeName fieldType = ClassName.get(internalElement.asType());

                        // Remove the diamond operator, if present ("Collection<Type>.class" is not allowed, but "Collection.class" is)
                        if (fieldType instanceof ParameterizedTypeName)
                            fieldType = ((ParameterizedTypeName) fieldType).rawType;

                        // Create the property
                        ParameterizedTypeName parameterizedField = ParameterizedTypeName.get(propertyClass, classType, fieldType);

                        entityClass.addField(
                                FieldSpec.builder(parameterizedField, fieldName)
                                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                        .initializer("new $T<>($T.class, $T.class, $T.class, $S);", propertyClass, classElement, currentElement, fieldType, fieldName)
                                        .build()
                        );
                    }

                    // Superclass
                    TypeMirror superClassTypeMirror = ((TypeElement)currentElement).getSuperclass();
                    currentElement = ((DeclaredType)superClassTypeMirror).asElement();
                }

                // Create class file
                JavaFile.builder(packageName, entityClass.build()).build().writeTo(getFiler());

            } catch (IOException e) {
                logError(e.getMessage());
            }
        }

        return true;
    }


    /**
     * Check for default constructor existence
     *
     * @param   element     entity element
     * @throws  ProcessorException if the class doesn't have a default constructor
     */
    private void checkForDefaultConstructor(TypeElement element) throws ProcessorException {
        for (ExecutableElement cons : ElementFilter.constructorsIn(element.getEnclosedElements())) {
            if (cons.getParameters().isEmpty())
                return;
        }

        // Couldn't find any default constructor here
        throw new ProcessorException("Entity " + element.getSimpleName() + " doesn't have a default constructor", element);
    }

}