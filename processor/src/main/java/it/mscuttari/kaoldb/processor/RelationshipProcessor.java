package it.mscuttari.kaoldb.processor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

/**
 * Analyze all the fields with {@link OneToOne}, {@link OneToMany}, {@link ManyToOne} or
 * {@link ManyToMany} annotations and check if the constraints are respected.
 *
 * {@link OneToOne} constraints: see {@link #checkOneToOneRelationship(Element)}
 * {@link OneToMany} constraints: see {@link #checkOneToManyRelationship(Element)}
 * {@link ManyToOne} constraints: see {@link #checkManyToOneRelationship(Element)}
 * {@link ManyToMany} constraints: see {@link #checkManyToManyRelationship(Element)}
 */
@SupportedAnnotationTypes({
        "it.mscuttari.kaoldb.annotations.OneToOne",
        "it.mscuttari.kaoldb.annotations.OneToMany",
        "it.mscuttari.kaoldb.annotations.ManyToOne",
        "it.mscuttari.kaoldb.annotations.ManyToMany"
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class RelationshipProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element field : roundEnv.getElementsAnnotatedWith(OneToOne.class)) {
            try {
                checkOneToOneRelationship(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(OneToMany.class)) {
            try {
                checkOneToManyRelationship(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(ManyToOne.class)) {
            try {
                checkManyToOneRelationship(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(ManyToMany.class)) {
            try {
                checkManyToManyRelationship(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        return true;
    }


    /**
     * Check field annotated with {@link OneToOne} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field is annotated with {@link JoinColumn}, {@link JoinColumns} or {@link JoinTable}.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkOneToOneRelationship(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check join columns presence
        OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);

        if (oneToOneAnnotation.mappedBy().isEmpty()) {
            JoinColumn joinColumnAnnotation   = field.getAnnotation(JoinColumn.class);
            JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
            JoinTable joinTableAnnotation     = field.getAnnotation(JoinTable.class);

            if (joinColumnAnnotation == null && joinColumnsAnnotation == null && joinTableAnnotation == null)
                throw new ProcessorException("@OneToOne relationship doesn't have @JoinColumn, @JoinColumns or @JoinTable annotation", field);
        }
    }


    /**
     * Check field annotated with {@link OneToMany} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field doesn't have {@link Column}, {@link JoinColumn}, {@link JoinColumns} or
     *      {@link JoinTable} annotations.
     *  -   The field is declared as a {@link Collection}.
     *  -   The {@link OneToMany#mappedBy()} field exists, is of correct type and is annotated
     *      with {@link ManyToOne}.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkOneToManyRelationship(Element field) throws ProcessorException {
        Elements elementUtils = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();


        // Check absence of @OneToOne, @ManyToOne and @ManyToMany annotations
        checkAnnotationCount(field);


        // Check absence of @Column, @JoinColumn, @JoinColumns and @JoinTable annotations
        Column columnAnnotation           = field.getAnnotation(Column.class);
        JoinColumn joinColumnAnnotation   = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation     = field.getAnnotation(JoinTable.class);

        if (columnAnnotation != null || joinColumnAnnotation != null || joinColumnsAnnotation != null || joinTableAnnotation != null)
            throw new ProcessorException("@OneToMany can't coexist with @Column, @JoinColumn, @JoinColumns or @JoinTable", field);


        // The field must be a Collection
        TypeMirror typeMirror = field.asType();
        TypeMirror collectionInterface = typeUtils.erasure(elementUtils.getTypeElement("java.util.Collection").asType());

        if (!typeUtils.erasure(typeMirror).equals(collectionInterface))
            throw new ProcessorException("Fields annotated with @OneToMany must be Collections", field);


        // Check mapping field
        OneToMany oneToManyAnnotation = field.getAnnotation(OneToMany.class);
        List<? extends TypeMirror> typeArguments = ((DeclaredType)typeMirror).getTypeArguments();

        if (typeArguments.size() == 0)
            throw new ProcessorException("Collection must specify the data type using the diamond operator", field);

        TypeMirror linkedType = typeArguments.get(0);
        Element linkedField = getClassField(linkedType, oneToManyAnnotation.mappedBy());

        if (linkedField == null)
            throw new ProcessorException("Field \"" + oneToManyAnnotation.mappedBy() + "\" not found in class \"" + typeUtils.asElement(linkedType).getSimpleName() + "\"", field);

        if (typeUtils.isAssignable(linkedField.asType(), field.asType()))
            throw new ProcessorException("Field \"" + oneToManyAnnotation.mappedBy() + "\" must be of type \"" + field.getEnclosingElement().getSimpleName() + "\"", linkedField);

        ManyToOne manyToOneAnnotation = linkedField.getAnnotation(ManyToOne.class);
        if (manyToOneAnnotation == null)
            throw new ProcessorException("Field \"" + oneToManyAnnotation.mappedBy() + "\" must have @ManyToOne annotation", linkedField);
    }


    /**
     * Check field annotated with {@link ManyToOne} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field is annotated with {@link JoinColumn}, {@link JoinColumns} or {@link JoinTable}.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkManyToOneRelationship(Element field) throws ProcessorException {
        // Check absence of @OneToOne, @OneToMany and @ManyToMany annotations
        checkAnnotationCount(field);

        // Check presence of @JoinColumn, @JoinColumns or @JoinTable
        JoinColumn joinColumnAnnotation   = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation     = field.getAnnotation(JoinTable.class);

        if (joinColumnAnnotation == null && joinColumnsAnnotation == null && joinTableAnnotation == null)
            throw new ProcessorException("@ManyToOne relationship doesn't have @JoinColumn, @JoinColumns or @JoinTable annotation", field);
    }


    /**
     * Check field annotated with {@link ManyToMany} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field is annotated with {@link JoinTable}.
     *  -   The field is declared as a {@link Collection}.
     *  -   The {@link OneToMany#mappedBy()} field exists, is of correct type and is annotated
     *      with {@link ManyToMany}.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraint are not respected
     */
    private void checkManyToManyRelationship(Element field) throws ProcessorException {
        Elements elementUtils = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();


        // Check absence of @OneToOne, @OneToMany and @ManyToOne annotations
        checkAnnotationCount(field);


        // The field must be a Collection
        TypeMirror typeMirror = field.asType();
        TypeMirror collectionInterface = typeUtils.erasure(elementUtils.getTypeElement("java.util.Collection").asType());

        if (!typeUtils.erasure(typeMirror).equals(collectionInterface))
            throw new ProcessorException("Fields annotated with @ManyToMany must be Collections", field);


        // Check the presence of the diamond operator
        List<? extends TypeMirror> collectionTypes = ((DeclaredType)typeMirror).getTypeArguments();

        if (collectionTypes.size() == 0)
            throw new ProcessorException("Collection must specify the data type using the diamond operator", field);

        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        if (manyToManyAnnotation.mappedBy().isEmpty()) {
            // Owning side of the relationship ("mappedBy" field is empty).
            // The field must have the @JoinTable annotation.

            if (field.getAnnotation(JoinTable.class) == null)
                throw new ProcessorException("@ManyToMany relationship must have a @JoinTable annotation", field);

        } else {
            // Non-owning side of the relationship
            TypeMirror linkedType = collectionTypes.get(0);
            Element linkedField = getClassField(linkedType, manyToManyAnnotation.mappedBy());

            if (linkedField == null)
                throw new ProcessorException("Field \"" + manyToManyAnnotation.mappedBy() + "\" not found in class \"" + typeUtils.asElement(linkedType).getSimpleName() + "\"", field);

            ManyToMany linkedFieldAnnotation = linkedField.getAnnotation(ManyToMany.class);

            if (linkedFieldAnnotation == null) {
                throw new ProcessorException("Field must have @ManyToMany annotation", linkedField);

            } else if (!linkedFieldAnnotation.mappedBy().isEmpty()) {
                throw new ProcessorException("Only one owning side of the relationship is allowed", linkedField);
            }

            // Check that the data type is compatible
            TypeMirror linkedTypeMirror = linkedField.asType();

            // Check that the linked field is a Collection
            if (!typeUtils.erasure(linkedTypeMirror).equals(collectionInterface))
                throw new ProcessorException("Fields annotated with @ManyToMany must be Collections", linkedField);

            // Check that the Collection type is compatible
            List<? extends TypeMirror> linkedCollectionArguments = ((DeclaredType) linkedTypeMirror).getTypeArguments();

            if (linkedCollectionArguments.size() == 0)
                throw new ProcessorException("Collection must specify the data type using the diamond operator", linkedField);

            TypeMirror linkedCollectionType = linkedCollectionArguments.get(0);

            if (!implementsInterface(linkedCollectionType, field.getEnclosingElement().asType()))
                throw new ProcessorException("Collection type must be of type " + typeUtils.asElement(field.getEnclosingElement().asType()).getSimpleName(), linkedField);
        }
    }


    /**
     * Check if the field has only one between {@link OneToOne}, {@link OneToMany},
     * {@link ManyToOne} and {@link ManyToMany} annotations.
     *
     * @param   field       field to be checked
     * @throws  ProcessorException if the constraint is not respected
     */
    private void checkAnnotationCount(Element field) throws ProcessorException {
        OneToOne columnAnnotation       = field.getAnnotation(OneToOne.class);
        OneToMany joinColumnAnnotation  = field.getAnnotation(OneToMany.class);
        ManyToOne joinColumnsAnnotation = field.getAnnotation(ManyToOne.class);
        ManyToMany joinTableAnnotation  = field.getAnnotation(ManyToMany.class);

        int annotationCount = 0;

        if (columnAnnotation      != null) annotationCount++;
        if (joinColumnAnnotation  != null) annotationCount++;
        if (joinColumnsAnnotation != null) annotationCount++;
        if (joinTableAnnotation   != null) annotationCount++;

        if (annotationCount > 1) {
            throw new ProcessorException("Only one annotation between @OneToOne, @OneToMany, @ManyToOne and @ManyToMany is allowed", field);
        }
    }


    /**
     * Get a class field given its name
     *
     * @param   classType       class type
     * @param   fieldName       field name
     *
     * @return  field (null if not found)
     */
    private Element getClassField(TypeMirror classType, String fieldName) {
        Element classElement = processingEnv.getTypeUtils().asElement(classType);

        for (Element field : classElement.getEnclosedElements()) {
            if (field.getKind() == ElementKind.FIELD && field.getSimpleName().toString().equals(fieldName))
                return field;
        }

        return null;
    }


    /**
     * Check if an element implements an interface
     *
     * @param   element     TypeMirror      element
     * @param   interf      TypeMirror      interface
     * @return  true if the element implements the interface
     */
    private boolean implementsInterface(TypeMirror element, TypeMirror interf) {
        return processingEnv.getTypeUtils().isAssignable(element, interf);
    }

}
