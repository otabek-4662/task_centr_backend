package com.taskcenter.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskUpdateRequestValidationTest {
    private final Validator validator;

    TaskUpdateRequestValidationTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void whenTitleIsEmpty_thenValidationFails() {
        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setTitle("");
        var violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void whenTitleIsNull_thenValidationPasses() {
        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setTitle(null);
        var violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }
}
