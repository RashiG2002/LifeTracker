package com.lifetracker.validation;

import com.lifetracker.entity.Task;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class FutureDateTimeValidator implements ConstraintValidator<FutureDateTime, Task> {

    @Override
    public void initialize(FutureDateTime constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Task task, ConstraintValidatorContext context) {
        if (task == null || task.getDueDate() == null || task.getDueTime() == null) {
            return true; // Let other validations handle nulls
        }

        LocalDateTime dueDateTime = LocalDateTime.of(task.getDueDate(), task.getDueTime());
        LocalDateTime now = LocalDateTime.now();

        return dueDateTime.isAfter(now);
    }
}