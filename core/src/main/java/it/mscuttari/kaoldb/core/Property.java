/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maps the fields of an entity
 *
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

    /** Field name (as specified in the entity class) */
    @NonNull final String fieldName;

    /**
     * Column annotation class (used for a rapid column type lookup).
     * It is one of {@link Column}, {@link JoinColumn}, {@link JoinColumns} and {@link JoinTable}.
     */
    @Nullable final Class<? extends Annotation> columnAnnotation;

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
    Property(@NonNull Class<M> entityClass, @NonNull Class<T> fieldType, @NonNull Field field) {
        this(entityClass, entityClass, fieldType, field.getName(), getColumnAnnotation(field), getRelationshipAnnotation(field));
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
                    @Nullable Class<? extends Annotation> columnAnnotation,
                    @Nullable Class<? extends Annotation> relationshipAnnotation) {

        this.entityClass            = checkNotNull(entityClass);
        this.fieldParentClass       = checkNotNull(fieldParentClass);
        this.fieldType              = checkNotNull(fieldType);
        this.fieldName              = checkNotNull(fieldName);
        this.columnAnnotation       = columnAnnotation;
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
     *
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
