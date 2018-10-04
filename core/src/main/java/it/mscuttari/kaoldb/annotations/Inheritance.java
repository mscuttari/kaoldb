package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines the inheritance strategy to be used for an entity class hierarchy
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Inheritance {

    /** The strategy to be used for the entity inheritance hierarchy */
    InheritanceType strategy() default InheritanceType.JOINED;

}

