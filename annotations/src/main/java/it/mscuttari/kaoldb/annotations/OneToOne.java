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

package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a single-valued association to another entity that has one-to-one multiplicity.
 * <p>If the relationship is bidirectional, the non-owning side must use the {@link OneToOne#mappedBy()}
 * element to specify the relationship field of the owning side.</p>
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface OneToOne {

    /**
     * The operations that must be cascaded to the target of the association.
     * By default no operations are cascaded.
     */
    // TODO: implement
    CascadeType[] cascade() default {};


    /**
     * Whether the association is optional.
     * If set to false then a non-null relationship must always exist.
     */
    boolean optional() default true;


    /**
     * The field that owns the relationship.
     * This element is only specified on the inverse (non-owning) side of the association.
     */
    String mappedBy() default "";


    /**
     * Whether to apply the remove operation to entities that have been removed from the
     * relationship and to cascade the remove operation to those entities.
     */
    // TODO: implement
    boolean orphanRemoval() default false;

}
