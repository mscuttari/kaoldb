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
