package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the discriminator column
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface DiscriminatorColumn {

    /**
     * The name of column to be used for the discriminator
     */
    String name();

    /**
     * The type of object/column to use as a class discriminator
     * Defaults to {@link DiscriminatorType#STRING}.
     */
    DiscriminatorType discriminatorType() default DiscriminatorType.STRING;

}
