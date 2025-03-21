package com.alibaba.fastjson2.adapter.jackson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonInclude {
    Include value() default Include.ALWAYS;

    enum Include {
        ALWAYS,
        NON_NULL,
        NON_ABSENT,
        NON_EMPTY,
        NON_DEFAULT,
        USE_DEFAULTS
    }
}
