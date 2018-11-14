package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a single-valued association to another entity that has one-to-one multiplicity.
 * If the relationship is bidirectional, the non-owning side must use the {@link OneToOne#mappedBy()}
 * element to specify the relationship field or property of the owning side.
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
