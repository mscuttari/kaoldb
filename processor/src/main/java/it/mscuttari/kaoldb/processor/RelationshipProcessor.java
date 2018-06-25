package it.mscuttari.kaoldb.processor;

import java.util.Collection;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
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
 * {@link ManyToMany} annotations
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

        for (Element field : roundEnv.getElementsAnnotatedWith(OneToOne.class))
            checkOneToOneRelationship(field);

        for (Element field : roundEnv.getElementsAnnotatedWith(OneToMany.class))
            checkOneToManyRelationship(field);

        for (Element field : roundEnv.getElementsAnnotatedWith(ManyToOne.class))
            checkManyToOneRelationship(field);

        for (Element field : roundEnv.getElementsAnnotatedWith(ManyToMany.class))
            checkManyToManyRelationship(field);

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
     * If any of the previous constraints is violated, the compile process is interrupted.
     *
     * @param   field       field element
     */
    private void checkOneToOneRelationship(Element field) {
        checkAnnotationCount(field);

        // Check join columns presence
        OneToOne oneToOneAnnotation = field.getAnnotation(OneToOne.class);

        if (oneToOneAnnotation.mappedBy().isEmpty()) {
            JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
            JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
            JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

            if (joinColumnAnnotation == null && joinColumnsAnnotation == null && joinTableAnnotation == null)
                logError("@OneToOne relationship doesn't have @JoinColumn, @JoinColumns or @JoinTable annotation", field);
        }
    }


    /**
     * Check field annotated with {@link OneToOne} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field doesn't have {@link Column}, {@link JoinColumn}, {@link JoinColumns} or
     *      {@link JoinTable} annotations
     *  -   The field implements the {@link Collection} interface.
     *
     * If any of the previous constraints is violated, the compile process is interrupted.
     *
     * @param   field       field element
     */
    private void checkOneToManyRelationship(Element field) {
        checkAnnotationCount(field);

        // Check absence of @Column, @JoinColumn, @JoinColumns and @JoinTable annotations
        Column columnAnnotation = field.getAnnotation(Column.class);
        JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        if (columnAnnotation != null || joinColumnAnnotation != null || joinColumnsAnnotation != null || joinTableAnnotation != null)
            logError("@OneToMany can't coexist with @Column, @JoinColumn, @JoinColumns or @JoinTable", field);

        // The field must be a Collection
        Elements elementUtils = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();

        TypeMirror collectionInterface = typeUtils.erasure(elementUtils.getTypeElement("java.util.Collection").asType());

        if (!implementsInterface(field.asType(), collectionInterface))
            logError("Fields annotated with @OneToMany must implement the Collection interface", field);
    }


    /**
     * Check field annotated with {@link OneToOne} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field is annotated with {@link JoinColumn}, {@link JoinColumns} or {@link JoinTable}.
     *
     * If any of the previous constraints is violated, the compile process is interrupted.
     *
     * @param   field       field element
     */
    private void checkManyToOneRelationship(Element field) {
        checkAnnotationCount(field);

        // Check join columns presence
        JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        if (joinColumnAnnotation == null && joinColumnsAnnotation == null && joinTableAnnotation == null)
            logError("@ManyToOne relationship doesn't have @JoinColumn, @JoinColumns or @JoinTable annotation", field);
    }


    /**
     * Check field annotated with {@link OneToOne} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link OneToOne},
     *      {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     *  -   The field is annotated with {@link JoinTable}.
     *
     * If any of the previous constraints is violated, the compile process is interrupted.
     *
     * @param   field       field element
     */
    private void checkManyToManyRelationship(Element field) {
        checkAnnotationCount(field);

        // Check join table presence
        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        if (joinTableAnnotation == null)
            logError("@ManyToMany relationship doesn't have @JoinTable annotation", field);
    }


    /**
     * Check if the field has only one between {@link OneToOne}, {@link OneToMany},
     * {@link ManyToOne} and {@link ManyToMany} annotations.
     * If the field has more than one annotation, the compile process is stopped.
     *
     * @param   field       field to be checked
     */
    private void checkAnnotationCount(Element field) {
        OneToOne columnAnnotation = field.getAnnotation(OneToOne.class);
        OneToMany joinColumnAnnotation = field.getAnnotation(OneToMany.class);
        ManyToOne joinColumnsAnnotation = field.getAnnotation(ManyToOne.class);
        ManyToMany joinTableAnnotation = field.getAnnotation(ManyToMany.class);

        int annotationCount = 0;

        if (columnAnnotation != null) annotationCount++;
        if (joinColumnAnnotation != null) annotationCount++;
        if (joinColumnsAnnotation != null) annotationCount++;
        if (joinTableAnnotation != null) annotationCount++;

        if (annotationCount > 1) {
            logError("Only one annotation between @OneToOne, @OneToMany, @ManyToOne and @ManyToMany is allowed", field);
        }
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
