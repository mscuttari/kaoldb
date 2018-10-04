package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the value of the discriminator column for entities of the given type.
 *
 * The {@link DiscriminatorValue} annotation can only be specified on a concrete entity class.
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface DiscriminatorValue {

    /**
     * The value that indicates that the row is an entity of the annotated entity type
     */
    String value();

}
