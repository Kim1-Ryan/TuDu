package com.kim.tudu_api.util.validation.min_length;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinLengthValidator.class)
public @interface MinLength {

    String message() default "Minimum length not met";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int value();
}
