package com.kim.tudu_api.util.validation.max_length;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxLengthValidator implements ConstraintValidator<MaxLength, String> {

    private int maxLength;

    @Override
    public void initialize(MaxLength constraintAnnotation) {
        maxLength = constraintAnnotation.value();
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return value.length() <= maxLength;
    }
}
