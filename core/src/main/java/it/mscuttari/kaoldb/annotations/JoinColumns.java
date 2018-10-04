package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines mapping for composite foreign keys.
 * This annotation groups {@link JoinColumn} annotations for the same relationship.
 *
 * @see JoinColumn
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JoinColumns {

    /**
     * The join columns that map the relationship.
     */
    JoinColumn[] value();

}
