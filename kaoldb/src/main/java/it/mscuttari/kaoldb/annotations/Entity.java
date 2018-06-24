package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the class is an entity
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Entity {

    /**
     * The name used to refer to the entity in queries
     * Defaults value is the unqualified name of the entity class
     */
    // TODO: implement
    String name() default "";


    /**
     * Unique table columns constraints
     */
    UniqueConstraint[] uniqueConstraints() default {};

}
