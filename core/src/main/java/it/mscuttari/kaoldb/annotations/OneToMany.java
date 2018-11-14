package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a many-valued association with one-to-many multiplicity.
 *
 * If the relationship is bidirectional, the {@link OneToMany#mappedBy()} element must be used to
 * specify the relationship field or property of the entity that is the owner of the relationship.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface OneToMany {

    /**
     * The operations that must be cascaded to the target of the association.
     * By default no operations are cascaded.
     */
    // TODO: implement
    CascadeType[] cascade() default {};


    /**
     * The field that owns the relationship
     */
    String mappedBy();


    /**
     * Whether to apply the remove operation to entities that have been removed from the
     * relationship and to cascade the remove operation to those entities.
     */
    // TODO: implement
    boolean orphanRemoval() default false;

}