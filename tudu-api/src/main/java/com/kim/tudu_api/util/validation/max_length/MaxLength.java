package com.kim.tudu_api.util.validation.max_length;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxLengthValidator.class)
public @interface MaxLength {

    String message() default "Maximum length exceeded";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int value();
}
