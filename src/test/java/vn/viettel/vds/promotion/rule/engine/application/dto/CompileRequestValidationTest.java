package vn.viettel.vds.promotion.rule.engine.application.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.rule.engine.TestFixtures;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompileRequest Validation Tests")
class CompileRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Nested
    @DisplayName("Valid requests")
    class ValidRequests {

        @Test
        @DisplayName("Should pass when all required fields present")
        void shouldPass_whenAllFieldsPresent() {
            // Given
            var request = TestFixtures.compileRequest("rule-1", 1);

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid ruleId")
    class InvalidRuleId {

        @Test
        @DisplayName("Should fail when ruleId is null")
        void shouldFail_whenRuleIdIsNull() {
            // Given
            var request = new CompileRequest();
            request.setVersion(1);
            request.setLogic("AND");
            request.setNodes(List.of());

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("ruleId"));
        }

        @Test
        @DisplayName("Should fail when ruleId is blank")
        void shouldFail_whenRuleIdIsBlank() {
            // Given
            var request = new CompileRequest();
            request.setRuleId("");
            request.setVersion(1);
            request.setLogic("AND");
            request.setNodes(List.of());

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid version")
    class InvalidVersion {

        @Test
        @DisplayName("Should fail when version is null")
        void shouldFail_whenVersionIsNull() {
            // Given
            var request = new CompileRequest();
            request.setRuleId("rule-1");
            request.setLogic("AND");
            request.setNodes(List.of());

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Should fail when version is zero or negative")
        void shouldFail_whenVersionIsZeroOrNegative() {
            // Given
            var request = new CompileRequest();
            request.setRuleId("rule-1");
            request.setVersion(0);
            request.setLogic("AND");
            request.setNodes(List.of());

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Invalid nodes")
    class InvalidNodes {

        @Test
        @DisplayName("Should fail when nodes is null")
        void shouldFail_whenNodesIsNull() {
            // Given
            var request = new CompileRequest();
            request.setRuleId("rule-1");
            request.setVersion(1);
            request.setLogic("AND");

            // When
            var violations = validator.validate(request);

            // Then
            assertThat(violations).isNotEmpty();
        }
    }
}
