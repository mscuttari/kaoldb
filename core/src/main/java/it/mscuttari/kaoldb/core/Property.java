package it.mscuttari.kaoldb.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import it.mscuttari.kaoldb.annotations.Column;
import it.mscuttari.kaoldb.annotations.JoinColumn;
import it.mscuttari.kaoldb.annotations.JoinColumns;
import it.mscuttari.kaoldb.annotations.JoinTable;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;

/**
 * @param   <M>     entity class
 * @param   <T>     data type
 */
public final class Property<M, T> {

    private Class<M> entityClass;
    private Class<? super M> fieldParentClass;
    private Class<T> fieldType;
    private String fieldName;
    private Class<? extends Annotation> columnAnnotation;
    private Class<? extends Annotation> relationshipAnnotation;


    /**
     * Constructor
     *
     * @param   entityClass         current entity class (the parent class is set to the set of the current class)
     * @param   fieldType           field type
     * @param   field               field
     */
    public Property(Class<M> entityClass, Class<T> fieldType, Field field) {
        this(entityClass, entityClass, fieldType, field.getName(), getColumnAnnotation(field), getRelationshipAnnotation(field));
    }


    /**
     * Constructor
     *
     * @param   entityClass             current entity class (the parent class is set to the set of the current class)
     * @param   fieldType               field type
     * @param   fieldName               field name
     * @param   columnAnnotation        column class
     * @param   relationshipAnnotation  relationship class
     */
    public Property(Class<M> entityClass, Class<T> fieldType, String fieldName, Class<? extends Annotation> columnAnnotation, Class<? extends Annotation> relationshipAnnotation) {
        this(entityClass, entityClass, fieldType, fieldName, columnAnnotation, relationshipAnnotation);
    }


    /**
     * Constructor
     *
     * @param   entityClass             current entity class (the parent class is set to the set of the current class)
     * @param   fieldType               field type
     * @param   fieldName               field name
     * @param   columnAnnotation        column class
     * @param   relationshipAnnotation  relationship class
     */
    public Property(Class<M> entityClass, Class<? super M> fieldParentClass, Class<T> fieldType, String fieldName, Class<? extends Annotation> columnAnnotation, Class<? extends Annotation> relationshipAnnotation) {
        this.entityClass = entityClass;
        this.fieldParentClass = fieldParentClass;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
        this.columnAnnotation = columnAnnotation;
        this.relationshipAnnotation = relationshipAnnotation;
    }


    /**
     * Get entity class
     *
     * @return  entity class
     */
    public Class<M> getEntityClass() {
        return entityClass;
    }


    /**
     * Get parent class
     *
     * @return  parent class the property originally belonged to
     */
    public Class<? super M> getFieldParentClass() {
        return fieldParentClass;
    }


    /**
     * Get field type
     *
     * @return  field class
     */
    public Class<T> getFieldType() {
        return fieldType;
    }


    /**
     * Get field name
     *
     * @return  field name
     */
    public String getFieldName() {
        return fieldName;
    }


    /**
     * Get column annotation type
     *
     * @return  column annotation type
     */
    public Class<? extends Annotation> getColumnAnnotation() {
        return columnAnnotation;
    }


    /**
     * Get relationship annotation class
     *
     * @return  relationship annotation class
     */
    public Class<? extends Annotation> getRelationshipAnnotation() {
        return relationshipAnnotation;
    }


    /**
     * Get field the property is linked to
     *
     * @return  field
     */
    public Field getField() {
        return EntityObject.getField(getEntityClass(), getFieldName());
    }


    /**
     * Get the column annotation class of a field
     *
     * @param   field   field to be analyzed
     * @return  class of the column annotation linked to the field
     *          (null if the field is not annotated with any column annotation)
     */
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
     * @param   field   field to be analyzed
     * @return  class of the relationship annotation linked to the field
     *          (null if the field is not annotated with any relationship annotation)
     */
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
