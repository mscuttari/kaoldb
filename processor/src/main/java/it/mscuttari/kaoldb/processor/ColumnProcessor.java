package it.mscuttari.kaoldb.processor;

import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;

/**
 * Analyze all the fields with {@link Column}, {@link JoinColumn}, {@link JoinColumns} or
 * {@link JoinTable} annotations
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

        for (Element field : roundEnv.getElementsAnnotatedWith(Column.class))
            checkAnnotationCount(field);

        for (Element field : roundEnv.getElementsAnnotatedWith(JoinColumn.class))
            checkAnnotationCount(field);

        for (Element field : roundEnv.getElementsAnnotatedWith(JoinColumns.class))
            checkAnnotationCount(field);

        for (Element field : roundEnv.getElementsAnnotatedWith(JoinTable.class))
            checkAnnotationCount(field);

        return true;
    }


    /**
     * Check if the field has only one between {@link Column}, {@link JoinColumn},
     * {@link JoinColumns} and {@link JoinTable} annotations.
     * If the field has more than one annotation, the compile process is stopped.
     *
     * @param   field       field to be checked
     */
    private void checkAnnotationCount(Element field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
        JoinColumns joinColumnsAnnotation = field.getAnnotation(JoinColumns.class);
        JoinTable joinTableAnnotation = field.getAnnotation(JoinTable.class);

        int annotationCount = 0;

        if (columnAnnotation != null) annotationCount++;
        if (joinColumnAnnotation != null) annotationCount++;
        if (joinColumnsAnnotation != null) annotationCount++;
        if (joinTableAnnotation != null) annotationCount++;

        if (annotationCount > 1) {
            logError("Only one annotation between @Column, @JoinColumn, @JoinColumns and @JoinTable is allowed", field);
        }
    }

}