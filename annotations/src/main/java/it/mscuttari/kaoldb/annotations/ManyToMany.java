package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToMany {

    Class targetEntity() default void.class;
    //CascadeType[] cascade() default {};
    //FetchType fetch() default FetchType.LAZY;
    String mappedBy() default "";

}