package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface ManyToOne {

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
    // TODO: implement
    boolean optional() default true;

}
