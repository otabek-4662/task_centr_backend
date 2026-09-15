package com.taskcenter.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.time.LocalDateTime;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureOrPresent.Validator.class)
@Documented
public @interface FutureOrPresent {
    String message() default "dueDate o'tmishdagi sana bo'lishi mumkin emas";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<FutureOrPresent, LocalDateTime> {
        @Override
        public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
            if (value == null) return true;
            return !value.isBefore(LocalDateTime.now());
        }
    }
}
