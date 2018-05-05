package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OneToOne {

    Class targetEntity() default void.class;
    //CascadeType[] cascade() default {};
    //FetchType fetch() default FetchType.EAGER;
    boolean optional() default true;
    String mappedBy() default "";
    boolean orphanRemoval() default false;

}
