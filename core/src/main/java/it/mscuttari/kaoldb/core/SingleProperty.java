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

/**
 * @param <M>   entity class
 * @param <T>   data type
 */
public final class SingleProperty<M, T> extends Property<M, T> {

    /**
     * Constructor
     *
     * @param entityClass   current entity class (the parent class is set to the set of the current class)
     * @param fieldType     field type
     * @param field         field the property is generated from
     */
    public SingleProperty(@NonNull Class<M> entityClass,
                          @NonNull Class<T> fieldType,
                          @NonNull Field field) {

        super(entityClass, fieldType, field);
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
    public SingleProperty(@NonNull Class<M> entityClass,
                          @NonNull Class<? super M> fieldParentClass,
                          @NonNull Class<T> fieldType,
                          @NonNull String fieldName,
                          @NonNull Class<? extends Annotation> columnAnnotation,
                          @Nullable Class<? extends Annotation> relationshipAnnotation) {

        super(entityClass, fieldParentClass, fieldType, fieldName, columnAnnotation, relationshipAnnotation);
    }

}