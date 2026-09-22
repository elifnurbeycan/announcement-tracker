package com.yasarbilgi.announcementtracker.dto;

import com.yasarbilgi.announcementtracker.dto.request.SubscriberRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberRequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void blankFullName_IsRejected() {
        SubscriberRequestDto request = SubscriberRequestDto.builder()
                .email("employee@example.com")
                .fullName(" ")
                .build();

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("fullName"));
    }

    @Test
    void validFullName_IsAccepted() {
        SubscriberRequestDto request = SubscriberRequestDto.builder()
                .email("employee@example.com")
                .fullName("Test User")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }
}
