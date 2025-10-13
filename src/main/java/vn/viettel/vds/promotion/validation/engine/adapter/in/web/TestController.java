package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/test")
@Tag(name = "Test", description = "Test endpoints for error handling validation")
public class TestController {

    @Operation(summary = "Test endpoint", description = "Simple test endpoint to verify service is running")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Validation Engine is running with updated promix-error starter");
    }

    @Operation(summary = "Test error handling", description = "Test endpoint that throws exceptions to validate error handling")
    @PostMapping("/error")
    public ResponseEntity<String> testError(@RequestParam String type) {
        switch (type) {
            case "runtime":
                throw new TestRuntimeException("Test runtime exception");
            case "illegal":
                throw new IllegalArgumentException("Test illegal argument exception");
            default:
                return ResponseEntity.ok("No error generated for type: " + type);
        }
    }

    @Operation(summary = "Test malformed JSON handling")
    @PostMapping("/json")
    public ResponseEntity<String> testJson(@RequestBody TestRequest request) {
        return ResponseEntity.ok("Received: " + request.getMessage());
    }

    public static class TestRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class TestRuntimeException extends RuntimeException {
        public TestRuntimeException(String message) {
            super(message);
        }
    }
}