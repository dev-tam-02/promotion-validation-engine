package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.engine.application.dto.ValidationRequest;
import vn.viettel.vds.promotion.validation.engine.application.dto.ValidationResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.in.ValidatePromotionUseCase;
import vn.viettel.vds.promotion.validation.engine.domain.model.Decision;
import vn.viettel.vds.promotion.validation.engine.domain.model.ReasonCode;

import java.util.List;
import java.util.Map;

@RestController
public class ValidationController {

    private final ValidatePromotionUseCase validatePromotionUseCase;

    public ValidationController(ValidatePromotionUseCase validatePromotionUseCase) {
        this.validatePromotionUseCase = validatePromotionUseCase;
    }

    @PostMapping("/validations")
    public ResponseEntity<ValidationResponse> validatePromotions(@RequestBody ValidationRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        if (request.customer() == null || request.order() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("customer and order are required"));
        }

        if (request.candidates() == null || request.candidates().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("candidates list is required and cannot be empty"));
        }

        try {
            ValidationResponse response = validatePromotionUseCase.validatePromotions(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal validation error: " + e.getMessage()));
        }
    }

    private ValidationResponse createErrorResponse(String errorMessage) {
        Decision errorDecision = new Decision();
        errorDecision.setValid(false);
        errorDecision.setReasons(List.of(new ReasonCode("VALIDATION_ERROR", Map.of("message", errorMessage))));

        return new ValidationResponse(List.of(errorDecision));
    }
}