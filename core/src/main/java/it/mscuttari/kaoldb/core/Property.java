package it.mscuttari.kaoldb.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

/**
 * @param <M>   entity class
 * @param <T>   data type
 */
public abstract class Property<M, T> {

    /** Current entity class */
    @NonNull final Class<M> entityClass;

    /** The class the property originally belonged to */
    @NonNull final Class<? super M> fieldParentClass;

    /** The class of the entity involved by the property */
    @NonNull final Class<T> fieldType;

    /** Field name (as specified in the model) */
    @NonNull final String fieldName;

    /**
     * Column annotation class (used for a rapid column type lookup).
     * It is one of {@link Column}, {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}.
     */
    @NonNull final Class<? extends Annotation> columnAnnotation;

    /**
     * Relationship annotation class (used for a rapid relationship type lookup).
     * It is one of {@link OneToOne}, {@link OneToMany}, {@link ManyToOne} and {@link ManyToMany}.
     * It can also be null if the field doesn't has any relationship with other entities.
     */
    @Nullable final Class<? extends Annotation> relationshipAnnotation;


    /**
     * Constructor
     *
     * @param entityClass   current entity class (the parent class is set to the set of the current class)
     * @param fieldType     field type
     * @param field         field the property is generated from
     */
    public Property(@NonNull Class<M> entityClass,
                    @NonNull Class<T> fieldType,
                    @NonNull Field field) {

        this(entityClass, entityClass, fieldType, field.getName(), getColumnAnnotation(field), getRelationshipAnnotation(field));
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
    public Property(@NonNull Class<M> entityClass,
                    @NonNull Class<T> fieldType,
                    @NonNull String fieldName,
                    @NonNull Class<? extends Annotation> columnAnnotation,
                    @Nullable Class<? extends Annotation> relationshipAnnotation) {

        this(entityClass, entityClass, fieldType, fieldName, columnAnnotation, relationshipAnnotation);
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
    public Property(@NonNull Class<M> entityClass,
                    @NonNull Class<? super M> fieldParentClass,
                    @NonNull Class<T> fieldType,
                    @NonNull String fieldName,
                    @NonNull Class<? extends Annotation> columnAnnotation,
                    @Nullable Class<? extends Annotation> relationshipAnnotation) {

        this.entityClass = entityClass;
        this.fieldParentClass = fieldParentClass;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
        this.columnAnnotation = columnAnnotation;
        this.relationshipAnnotation = relationshipAnnotation;
    }


    /**
     * Get field the property is linked to
     *
     * @return field
     */
    Field getField() {
        return EntityObject.getField(entityClass, fieldName);
    }


    /**
     * Get the column annotation class of a field
     *
     * @param field     field to be analyzed
     * @return class of the column annotation linked to the field
     *         (null if the field is not annotated with any column annotation)
     */
    @Nullable
    private static Class<? extends Annotation> getColumnAnnotation(Field field) {
        if (field.isAnnotationPresent(Column.class))
            return Column.class;

        if (field.isAnnotationPresent(JoinColumn.class))
            return JoinColumn.class;

        if (field.isAnnotationPresent(JoinColumns.class))
            return JoinColumns.class;

        if (field.isAnnotationPresent(JoinTable.class))
            return JoinTable.class;

        return null;
    }


    /**
     * Get the relationship annotation class of a field
     *
     * It can be one of {@link OneToOne}, {@link OneToMany}, {@link ManyToOne} and
     * {@link ManyToMany}. It can also be null if none of the previous are specified.
     *
     * @param   field   field to be analyzed
     * @return  class of the relationship annotation linked to the field
     *          (null if the field is not annotated with any relationship annotation)
     */
    @Nullable
    private static Class<? extends Annotation> getRelationshipAnnotation(Field field) {
        if (field.isAnnotationPresent(OneToOne.class))
            return OneToOne.class;

        if (field.isAnnotationPresent(OneToMany.class))
            return OneToMany.class;

        if (field.isAnnotationPresent(ManyToOne.class))
            return ManyToOne.class;

        if (field.isAnnotationPresent(ManyToMany.class))
            return ManyToMany.class;

        return null;
    }

}
