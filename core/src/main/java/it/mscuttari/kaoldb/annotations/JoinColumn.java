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
 * Used to specify the mapped column for a persistent property or field
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JoinColumn {

    /**
     * The name of the column
     * Defaults to the property or field name.
     */
    String name() default "";


    /**
     * The name of the column referenced by this foreign key column.
     */
    String referencedColumnName();


    /**
     * Whether the database column is nullable
     */
    boolean nullable() default true;


    /**
     * Whether the column is a unique key
     *
     * This is a shortcut for the {@link UniqueConstraint} annotation at the entity level and is
     * useful for when the unique key constraint corresponds to only a single column.
     * This constraint applies in addition to any constraint entailed by primary key mapping and
     * to constraints specified at the table level.
     */
    boolean unique() default false;


    /**
     * Default value of the column
     */
    String defaultValue() default "";


    /**
     * The SQL fragment that is used when generating the DDL for the column.
     * Defaults to the generated SQL to create a column of the inferred type.
     */
    String columnDefinition() default "";

}
