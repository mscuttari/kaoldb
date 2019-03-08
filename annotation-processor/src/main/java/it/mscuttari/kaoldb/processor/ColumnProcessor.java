package it.mscuttari.kaoldb.processor;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

/**
 * Analyze all the fields with {@link Column}, {@link JoinColumn}, {@link JoinColumns} or
 * {@link JoinTable} annotations and check if the constraints are respected.
 *
 * {@link Column} constraints: see {@link #checkColumn(Element)}
 * {@link JoinColumn} constraints: see {@link #checkJoinColumn(Element)}
 * {@link JoinColumns} constraints: see {@link #checkJoinColumns(Element)}
 * {@link JoinTable} constraints: see {@link #checkJoinTable(Element)}
 */
@SupportedAnnotationTypes({
        "it.mscuttari.kaoldb.annotations.Column",
        "it.mscuttari.kaoldb.annotations.JoinColumn",
        "it.mscuttari.kaoldb.annotations.JoinColumns",
        "it.mscuttari.kaoldb.annotations.JoinTable"
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class ColumnProcessor extends AbstractAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element field : roundEnv.getElementsAnnotatedWith(Column.class)) {
            try {
                checkColumn(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(JoinColumn.class)) {
            try {
                checkJoinColumn(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(JoinColumns.class)) {
            try {
                checkJoinColumns(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        for (Element field : roundEnv.getElementsAnnotatedWith(JoinTable.class)) {
            try {
                checkJoinTable(field);
            } catch (ProcessorException e) {
                logError(e.getMessage(), e.getElement());
            }
        }

        return true;
    }


    /**
     * Check field annotated with {@link Column} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link Column},
     *      {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}.
     *  -   The field doesn't have {@link OneToOne}, {@link OneToMany}, {@link ManyToOne} or
     *      {@link ManyToMany} annotations.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkColumn(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check relationship absence
        OneToOne oneToOneAnnotation     = field.getAnnotation(OneToOne.class);
        OneToMany oneToManyAnnotation   = field.getAnnotation(OneToMany.class);
        ManyToOne manyToOneAnnotation   = field.getAnnotation(ManyToOne.class);
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        if (oneToOneAnnotation != null || oneToManyAnnotation != null || manyToOneAnnotation != null || manyToManyAnnotation != null) {
            throw new ProcessorException("Field annotated with @Column can't have @OneToOne, @OneToMany, @ManyToOne or @ManyToMany annotations", field);
        }
    }


    /**
     * Check field annotated with {@link JoinColumn} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link Column},
     *      {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}.
     *  -   The field is annotated with {@link OneToOne}, {@link OneToMany}, {@link ManyToOne} or
     *      {@link ManyToMany} annotations.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkJoinColumn(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check relationship
        OneToOne oneToOneAnnotation     = field.getAnnotation(OneToOne.class);
        OneToMany oneToManyAnnotation   = field.getAnnotation(OneToMany.class);
        ManyToOne manyToOneAnnotation   = field.getAnnotation(ManyToOne.class);
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        if (oneToOneAnnotation == null && oneToManyAnnotation == null && manyToOneAnnotation == null && manyToManyAnnotation == null) {
            throw new ProcessorException("Field annotated with @JoinColumn must be annotated with @OneToOne, @OneToMany, @ManyToOne or @ManyToMany", field);
        }
    }


    /**
     * Check field annotated with {@link JoinColumns} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link Column},
     *      {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}.
     *  -   The field is annotated with {@link OneToOne}, {@link OneToMany}, {@link ManyToOne} or
     *      {@link ManyToMany} annotations.
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkJoinColumns(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check relationship
        OneToOne oneToOneAnnotation     = field.getAnnotation(OneToOne.class);
        OneToMany oneToManyAnnotation   = field.getAnnotation(OneToMany.class);
        ManyToOne manyToOneAnnotation   = field.getAnnotation(ManyToOne.class);
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        if (oneToOneAnnotation == null && oneToManyAnnotation == null && manyToOneAnnotation == null && manyToManyAnnotation == null) {
            throw new ProcessorException("Field annotated with @JoinColumn must be annotated with @OneToOne, @OneToMany, @ManyToOne or @ManyToMany", field);
        }
    }


    /**
     * Check field annotated with {@link JoinTable} annotation.
     *
     * It ensures the following constraints are respected:
     *  -   The field doesn't have more than one annotation between {@link Column},
     *      {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}.
     *  -   The field is annotated with {@link OneToOne}, {@link OneToMany}, {@link ManyToOne} or
     *      {@link ManyToMany} annotations.
     *  -   The {@link JoinTable#joinClass()} is the same of the declaring class.
     *  -   The {@link JoinTable#inverseJoinClass()} is the same of the field type (in case of
     *      {@link OneToOne} and {@link ManyToOne}) or the same of Collection elements type (in
     *      case of {@link OneToMany} and {@link ManyToMany}).
     *
     * @param   field       field element
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkJoinTable(Element field) throws ProcessorException {
        checkAnnotationCount(field);

        // Check relationship
        OneToOne oneToOneAnnotation     = field.getAnnotation(OneToOne.class);
        OneToMany oneToManyAnnotation   = field.getAnnotation(OneToMany.class);
        ManyToOne manyToOneAnnotation   = field.getAnnotation(ManyToOne.class);
        ManyToMany manyToManyAnnotation = field.getAnnotation(ManyToMany.class);

        if (oneToOneAnnotation == null && oneToManyAnnotation == null && manyToOneAnnotation == null && manyToManyAnnotation == null) {
            throw new ProcessorException("Field annotated with @JoinColumn must be annotated with @OneToOne, @OneToMany, @ManyToOne or @ManyToMany", field);
        }

        // Check the joinClass and the inverseJoinClass values
        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        // Direct join class
        TypeMirror joinClass = null;

        try {
            joinTableAnnotation.joinClass();
        } catch (MirroredTypeException e) {
            joinClass = e.getTypeMirror();
        }

        TypeMirror enclosingClass = field.getEnclosingElement().asType();

        if (!enclosingClass.equals(joinClass)) {
            throw new ProcessorException("Invalid joinClass: expected " + enclosingClass + ", found " + joinClass, field);
        }

        // Inverse join class
        TypeMirror inverseJoinClass = null;

        try {
            joinTableAnnotation.inverseJoinClass();
        } catch (MirroredTypeException e) {
            inverseJoinClass = e.getTypeMirror();
        }

        TypeMirror fieldType = field.asType();

        if (oneToOneAnnotation != null || manyToOneAnnotation != null) {
            if (!fieldType.equals(inverseJoinClass)) {
                throw new ProcessorException("Invalid inverseJoinClass: expected " + fieldType + ", found " + inverseJoinClass, field);
            }

        } else {
            List<? extends TypeMirror> collectionInterfaceArguments = ((DeclaredType) fieldType).getTypeArguments();

            if (collectionInterfaceArguments.size() != 0) {
                TypeMirror linkedClass = collectionInterfaceArguments.get(0);

                if (!linkedClass.equals(inverseJoinClass)) {
                    throw new ProcessorException("Invalid inverseJoinClass: expected " + linkedClass + ", found " + inverseJoinClass, field);
                }
            }
        }
    }


    /**
     * Check if the field has only one between {@link Column}, {@link JoinColumn},
     * {@link JoinColumns} and {@link JoinTable} annotations.
     *
     * @param   field       field to be checked
     * @throws  ProcessorException if some constraints are not respected
     */
    private void checkAnnotationCount(Element field) throws ProcessorException{
        Column columnAnnotation           = field.getAnnotation(Column.class);
        JoinColumn joinColumnAnnotation   = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation     = field.getAnnotation(JoinTable.class);

        int annotationCount = 0;

        if (columnAnnotation      != null) annotationCount++;
        if (joinColumnAnnotation  != null) annotationCount++;
        if (joinColumnsAnnotation != null) annotationCount++;
        if (joinTableAnnotation   != null) annotationCount++;

        if (annotationCount > 1) {
            throw new ProcessorException("Only one annotation between @Column, @JoinColumn, @JoinColumns and @JoinTable is allowed", field);
        }
    }

}