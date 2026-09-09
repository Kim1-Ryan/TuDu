package com.kim.tudu_api.util.validation.min_length;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinLengthValidator implements ConstraintValidator<MinLength, String> {

    private int minLength;

    @Override
    public void initialize(MinLength constraintAnnotation) {
        minLength = constraintAnnotation.value();
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return value.length() >= minLength;
    }
}
