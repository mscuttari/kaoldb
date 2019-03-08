package it.mscuttari.kaoldb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

public @interface JoinTable {

    String name();
    Class<?> joinClass();
    JoinColumn[] joinColumns();
    Class<?> inverseJoinClass();
    JoinColumn[] inverseJoinColumns();
    //UniqueConstraint[] uniqueConstraints() default {};

}
