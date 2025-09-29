package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileJobResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.validation.engine.application.dto.CompileResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.in.CompileUseCase;

import java.util.List;

@RestController
@RequestMapping("/v1/compiler")
public class CompileController {

    @Autowired
    private CompileUseCase compileUseCase;

    @PostMapping("/compile")
    public ResponseEntity<CompileResponse> compile(@Valid @RequestBody CompileRequest request) {
        try {
            CompileResponse response = compileUseCase.compile(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
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