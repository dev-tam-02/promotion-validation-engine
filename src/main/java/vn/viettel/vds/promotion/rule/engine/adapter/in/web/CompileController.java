package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.CompileResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileJobResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.in.CompileUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated Use {@link CompilationController} at /v1/compile instead.
 * This controller is kept for backward compatibility with existing Feign clients.
 * New integrations should use /v1/compile (compile), /v1/compile/warmup (warmup),
 * and /v1/compile/bundle/{hash}/status (status).
 */
@Deprecated(since = "2026-03", forRemoval = true)
@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/compiler")
public class CompileController {

    private final CompileUseCase compileUseCase;
    private final ObjectStoragePort objectStoragePort;

    public CompileController(CompileUseCase compileUseCase, ObjectStoragePort objectStoragePort) {
        this.compileUseCase = compileUseCase;
        this.objectStoragePort = objectStoragePort;
    }

    @PostMapping("/compile")
    public ResponseEntity<CompileResponse> compile(@Valid @RequestBody CompileRequest request) {
        try {
            vn.viettel.vds.promotion.rule.engine.application.dto.CompileResponse useCaseResponse =
                    compileUseCase.compile(request);

            // Map application DTO to web DTO with ok field
            CompileResponse webResponse = new CompileResponse();
            webResponse.setOk(true);
            webResponse.setBundleHash(useCaseResponse.getBundleHash());
            webResponse.setArtifactSize(useCaseResponse.getSize());
            webResponse.setLogs(useCaseResponse.getLogs() != null ? useCaseResponse.getLogs() : new ArrayList<>());
            webResponse.setDrlContent(useCaseResponse.getDrlContent());

            if (useCaseResponse.getEngine() != null) {
                webResponse.setEngineVersion(useCaseResponse.getEngine().getDroolsVersion());
            }

            // Load artifactBytes from storage for warmup - this is MANDATORY for proper execution
            String artifactKey = "bundles/" + useCaseResponse.getBundleHash().replace("sha256:", "") + ".kjar";
            byte[] artifactBytes = objectStoragePort.retrieve(artifactKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to retrieve compiled artifact from storage. Bundle may not be usable for execution. " +
                                    "BundleHash: " + useCaseResponse.getBundleHash() + ", Key: " + artifactKey));

            webResponse.setArtifactBytes(artifactBytes);
            webResponse.setErrors(new ArrayList<>());

            return ResponseEntity.status(HttpStatus.CREATED).body(webResponse);
        } catch (IllegalStateException e) {
            CompileResponse errorResponse = new CompileResponse();
            errorResponse.setOk(false);
            errorResponse.setErrors(List.of("Compilation already in progress"));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (IllegalArgumentException e) {
            CompileResponse errorResponse = new CompileResponse();
            errorResponse.setOk(false);
            errorResponse.setErrors(List.of("Invalid request: " + e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            // Log the full error for debugging
            org.slf4j.LoggerFactory.getLogger(CompileController.class)
                    .error("Compilation failed for ruleId={}, version={}, errorType={}",
                            request.getRuleId(), request.getVersion(),
                            e.getClass().getSimpleName(), e);

            CompileResponse errorResponse = new CompileResponse();
            errorResponse.setOk(false);

            // Ensure error message is never null
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "Compilation failed: " + e.getClass().getSimpleName();

                // Try to get cause message if available
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    errorMessage += " - " + e.getCause().getMessage();
                }
            } else {
                errorMessage = "Compilation failed: " + errorMessage;
            }

            errorResponse.setErrors(List.of(errorMessage));
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
        }
    }

    @GetMapping("/compile-jobs")
    public ResponseEntity<List<CompileJobResponse>> getCompileJobs(
            @RequestParam(required = false) String ruleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<CompileJobResponse> jobs = compileUseCase.getCompileJobs(
                    ruleId, status, from, to, page, size);
            return ResponseEntity.ok(jobs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/compile-jobs/{jobId}")
    public ResponseEntity<CompileJobResponse> getCompileJob(@PathVariable String jobId) {
        try {
            CompileJobResponse job = compileUseCase.getCompileJob(jobId);
            return ResponseEntity.ok(job);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}