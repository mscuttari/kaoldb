package it.mscuttari.kaoldb.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @param <M>   entity class
 * @param <T>   data type
 */
public final class CollectionProperty<M, T> extends Property<M, T> {

    /**
     * Constructor
     *
     * @param entityClass   current entity class (the parent class is set to the set of the current class)
     * @param fieldType     field type
     * @param field         field
     */
    public CollectionProperty(@NonNull Class<M> entityClass,
                              @NonNull Class<T> fieldType,
                              @NonNull Field field) {

        super(entityClass, fieldType, field);
    }


    /**
     * Constructor
     *
     * @param entityClass               current entity class (the parent class is set to the set of the current class)
     * @param fieldType                 field type
     * @param fieldName                 field name
     * @param columnAnnotation          column class
     * @param relationshipAnnotation    relationship class
     */
    public CollectionProperty(@NonNull Class<M> entityClass,
                              @NonNull Class<T> fieldType,
                              @NonNull String fieldName,
                              @NonNull Class<? extends Annotation> columnAnnotation,
                              @Nullable Class<? extends Annotation> relationshipAnnotation) {

        super(entityClass, fieldType, fieldName, columnAnnotation, relationshipAnnotation);
    }


    /**
     * Constructor
     *
     * @param entityClass               current entity class
     * @param fieldParentClass          the class the property really belongs to
     * @param fieldType                 field type
     * @param fieldName                 field name
     * @param columnAnnotation          column class
     * @param relationshipAnnotation    relationship class
     */
    public CollectionProperty(@NonNull Class<M> entityClass,
                              @NonNull Class<? super M> fieldParentClass,
                              @NonNull Class<T> fieldType,
                              @NonNull String fieldName,
                              @NonNull Class<? extends Annotation> columnAnnotation,
                              @Nullable Class<? extends Annotation> relationshipAnnotation) {

        super(entityClass, fieldParentClass, fieldType, fieldName, columnAnnotation, relationshipAnnotation);
    }

}
