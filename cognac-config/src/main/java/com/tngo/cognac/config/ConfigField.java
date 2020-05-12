package com.tngo.cognac.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(FIELD)
public @interface ConfigField {

    String value();

    ConfigScope scope() default ConfigScope.RELATIVE;
}
