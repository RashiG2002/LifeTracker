package com.lifetracker.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class FutureTimeValidator implements ConstraintValidator<FutureTime, LocalTime> {

    @Override
    public void initialize(FutureTime constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(LocalTime time, ConstraintValidatorContext context) {
        if (time == null) {
            return true; // Let @NotNull handle null validation
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime selectedDateTime = LocalDateTime.of(LocalDate.now(), time);

        // If the time is for today, check if it's in the future
        if (selectedDateTime.isBefore(now)) {
            return false;
        }

        return true;
    }
}