package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface ManyToMany {

    /**
     * The operations that must be cascaded to the target of the association.
     * Defaults to no operations being cascaded.
     */
    // TODO: implement
    CascadeType[] cascade() default {};


    /**
     * The field that owns the relationship.
     */
    String mappedBy() default "";

}