package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.CompileResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileJobResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.validation.engine.application.port.in.CompileUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.out.ObjectStoragePort;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/compiler")
public class CompileController {

    @Autowired
    private CompileUseCase compileUseCase;

    @Autowired
    private ObjectStoragePort objectStoragePort;

    @PostMapping("/compile")
    public ResponseEntity<CompileResponse> compile(@Valid @RequestBody CompileRequest request) {
        try {
            vn.viettel.vds.promotion.validation.engine.application.dto.CompileResponse useCaseResponse =
                    compileUseCase.compile(request);

            // Map application DTO to web DTO with ok field
            CompileResponse webResponse = new CompileResponse();
            webResponse.setOk(true);
            webResponse.setBundleHash(useCaseResponse.getBundleHash());
            webResponse.setArtifactSize(useCaseResponse.getSize());
            webResponse.setLogs(useCaseResponse.getLogs() != null ? useCaseResponse.getLogs() : new ArrayList<>());

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
            CompileResponse errorResponse = new CompileResponse();
            errorResponse.setOk(false);
            errorResponse.setErrors(List.of("Compilation failed: " + e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
        }
    }

    @GetMapping("/compile-jobs")
    public ResponseEntity<List<CompileJobResponse>> getCompileJobs(
            @RequestParam String tenantId,
            @RequestParam(required = false) String ruleId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<CompileJobResponse> jobs = compileUseCase.getCompileJobs(
                    tenantId, ruleId, status, from, to, page, size);
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