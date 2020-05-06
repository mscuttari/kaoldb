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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import androidx.annotation.NonNull;
import it.mscuttari.kaoldb.annotations.ManyToMany;
import it.mscuttari.kaoldb.annotations.ManyToOne;
import it.mscuttari.kaoldb.annotations.OneToMany;
import it.mscuttari.kaoldb.annotations.OneToOne;
import it.mscuttari.kaoldb.exceptions.MappingException;

import static it.mscuttari.kaoldb.core.Relationship.RelationshipType.*;

/**
 * Maps a relationship.
 * <p>The possible combinations of column and relationship annotation specified on {@link #field} are
 * the following ones:
 * <pre>
 *                          | Column | JoinColumn | JoinColumns | JoinTable
 * OneToOne (mapping)       |   No   |    Yes     |     Yes     |    Yes
 * OneToOne (non-mapping)   |   No   |    No      |     No      |    No
 * OneToMany                |   No   |    No      |     No      |    No
 * ManyToOne                |   No   |    Yes     |     Yes     |    Yes
 * ManyToMany (mapping)     |   No   |    No      |     No      |    Yes
 * ManyToMany (non-mapping) |   No   |    No      |     No      |    No</pre>
 * </p>
 */
class Relationship {

    public enum RelationshipType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE,
        MANY_TO_MANY
    }

    /** Field declaring the relationship */
    @NonNull public final Field field;

    /** Annotation of the relationship */
    @NonNull public final RelationshipType type;

    /** Class declaring {@link #field} */
    @NonNull public final Class<?> local;

    /** Other side of the relationship */
    @NonNull public final Class<?> linked;

    /** Whether {@link #local} is the owning side or not */
    public final boolean owning;

    /** Field that owns the relationship */
    @NonNull public final Field mappingField;

    /**
     * Constructor.
     *
     * @param db        database
     * @param field     field declaring the relationship
     */
    public Relationship(@NonNull DatabaseObject db, @NonNull Field field) {
        this.field = field;
        this.local = field.getDeclaringClass();

        if (field.isAnnotationPresent(OneToOne.class)) {
            OneToOne annotation = field.getAnnotation(OneToOne.class);

            type = ONE_TO_ONE;
            owning = annotation.mappedBy().isEmpty();
            linked = field.getType();
            mappingField = owning ? field : db.getEntity(linked).getField(annotation.mappedBy());

        } else if (field.isAnnotationPresent(OneToMany.class)) {
            OneToMany annotation = field.getAnnotation(OneToMany.class);

            type = RelationshipType.ONE_TO_MANY;
            owning = false;
            linked = getCollectionType(field);
            mappingField = db.getEntity(linked).getField(annotation.mappedBy());

        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            type = RelationshipType.MANY_TO_ONE;
            owning = true;
            linked = field.getType();
            mappingField = field;

        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            ManyToMany annotation = field.getAnnotation(ManyToMany.class);

            type = RelationshipType.MANY_TO_MANY;
            owning = annotation.mappedBy().isEmpty();
            linked = getCollectionType(field);
            mappingField = owning ? field : db.getEntity(linked).getField(annotation.mappedBy());

        } else {
            throw new MappingException("No relationship annotation found on field \"" + field.getName() + "\"");
        }

        // Print a warning if the field type is incompatible with lazy load
        if (!isLazilyInitializable()) {
            LogUtils.w("[Relationship \"" + field.getName() + "\"] declared type " + field.getType().getSimpleName() + " is incompatible with lazy loading");
        }
    }

    /**
     * Get the data type of a field that implements the {@link Collection} interface.
     *
     * @param field     field implementing the Collection interface
     * @return data type (class specified in the diamond operator of the Collection)
     */
    private Class<?> getCollectionType(Field field) {
        Stack<Type> elements = new Stack<>();
        elements.push(field.getGenericType());

        // Search the Collection interface
        while (!elements.isEmpty()) {
            Type element = elements.peek();

            // The first level class may not be parameterized but be an extension or an
            // extension of a Collection and its type may be coded in the Collection itself
            Class<?> rawClass = getTypeRawClass(element);

            if (!Collection.class.isAssignableFrom(rawClass)) {
                elements.pop();
                continue;
            }

            if (element instanceof ParameterizedType && rawClass.equals(Collection.class)) {
                // Get the data type. It may be a generic, and in that case we need to map it back
                // to the effective class.

                Type dataType = ((ParameterizedType) element).getActualTypeArguments()[0];

                while (!elements.empty()) {
                    Type parent = elements.pop();

                    // Get the position of the generic in the parent class declaration
                    int parentGenericIndex = 0;
                    Type[] parentGenerics = getTypeRawClass(parent).getTypeParameters();

                    for (Type generic : parentGenerics) {
                        if (generic.toString().equals(dataType.toString()))
                            break;

                        parentGenericIndex++;
                    }

                    // Generic is not propagated to child
                    if (parentGenericIndex >= parentGenerics.length)
                        return getTypeRawClass(dataType);

                    // Last class reached (which by the way is the one declared for the field)
                    if (elements.empty()) {
                        return getTypeRawClass(((ParameterizedType) field.getGenericType()).getActualTypeArguments()[parentGenericIndex]);
                    }

                    // Get the label used in the child for the same generic
                    Type child = elements.peek();
                    Class<?> childClass = getTypeRawClass(child);

                    Type childSuperType = childClass.getGenericSuperclass();
                    Class<?> childSuperClass = getTypeRawClass(childSuperType);

                    Type[] childInterfaces = childClass.getGenericInterfaces();
                    Class<?> parentClass = getTypeRawClass(parent);

                    if (parentClass.equals(childSuperClass)) {
                        dataType = ((ParameterizedType) childSuperType).getActualTypeArguments()[parentGenericIndex];

                    } else {
                        for (Type childInterface : childInterfaces) {
                            Class<?> childInterfaceClass = getTypeRawClass(childInterface);

                            if (parentClass.equals(childInterfaceClass)) {
                                dataType = ((ParameterizedType) childInterface).getActualTypeArguments()[parentGenericIndex];
                                break;
                            }
                        }
                    }
                }

                return getTypeRawClass(dataType);
            }

            // Add the superclass to the stack
            Type superType = rawClass.getGenericSuperclass();

            if (superType != null) {
                elements.add(superType);
            }

            // Add the interfaces to the stack
            Type[] interfaces = rawClass.getGenericInterfaces();
            elements.addAll(Arrays.asList(interfaces));
        }

        throw new MappingException("Field doesn't implement the Collection interface");
    }

    /**
     * Get the raw class (thus without any diamond operator) of a Type.
     *
     * @param type  Type representing the class
     * @return class erasure
     */
    private Class<?> getTypeRawClass(Type type) {
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
            return (Class<?>) type;
        }
    }

    /**
     * Get the class that owns the relationship.
     *
     * @return  owning class
     */
    public Class<?> getOwningClass() {
        return owning ? local : linked;
    }

    /**
     * Get the class that doesn't own the relationship.
     *
     * @return non owning class
     */
    public Class<?> getNonOwningClass() {
        return owning ? linked : local;
    }

    /**
     * Determine whether the lazy initialization can be done, according to declared field class.<br>
     * In fact, if the field isn't declared as a {@link Collections}, {@link List} or {@link Set},
     * the lazy initialization wrapper can not be assigned to the field.
     *
     * @return <code>true</code> if the lazy initialization is allowed; <code>false</code> otherwise
     * @see LazyCollection
     */
    public boolean isLazilyInitializable() {
        Class<?> fieldClass = field.getType();

        return fieldClass.equals(Collection.class) ||
                fieldClass.equals(List.class) ||
                fieldClass.equals(Set.class);
    }

}
