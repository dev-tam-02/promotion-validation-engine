package vn.viettel.vds.promotion.rule.engine.application.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExecuteRequest Validation Tests")
class ExecuteRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("Valid requests")
    class ValidRequests {

        @Test
        @DisplayName("Should pass when all required fields present")
        void shouldPass_whenAllFieldsPresent() {
            // Given
            var bundle = new ExecuteRequest.Bundle("sha256:abc", 1, 1);
            var context = Map.<String, Object>of("now", "2026-01-01T00:00:00Z");
            var request = new ExecuteRequest(bundle, context, null);

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid bundle")
    class InvalidBundle {

        @Test
        @DisplayName("Should fail when bundle is null")
        void shouldFail_whenBundleIsNull() {
            // Given
            var context = Map.<String, Object>of("now", "2026-01-01T00:00:00Z");
            var request = new ExecuteRequest(null, context, null);

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail when bundle hash is blank")
        void shouldFail_whenBundleHashIsBlank() {
            // Given
            var bundle = new ExecuteRequest.Bundle("", 1, 1);
            var context = Map.<String, Object>of("now", "2026-01-01T00:00:00Z");
            var request = new ExecuteRequest(bundle, context, null);

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid context")
    class InvalidContext {

        @Test
        @DisplayName("Should fail when context is null")
        void shouldFail_whenContextIsNull() {
            // Given
            var bundle = new ExecuteRequest.Bundle("sha256:abc", 1, 1);
            var request = new ExecuteRequest(bundle, null, null);

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
        }
    }
}
