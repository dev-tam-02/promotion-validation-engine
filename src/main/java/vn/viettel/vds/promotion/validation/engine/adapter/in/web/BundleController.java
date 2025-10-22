package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.engine.application.dto.BundleMetadataResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.LatestBundleResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.WarmupRequest;
import vn.viettel.vds.promotion.validation.engine.application.port.in.BundleLookupUseCase;
import vn.viettel.vds.promotion.validation.engine.domain.exception.BundleNotFoundException;

@RestController
@RequestMapping("${spring.application.context-path}/v1")
public class BundleController {

    @Autowired
    private BundleLookupUseCase bundleLookupUseCase;

    @GetMapping("/bundles/{bundleHash}")
    public ResponseEntity<BundleMetadataResponse> getBundleMetadata(@PathVariable String bundleHash) {
        try {
            BundleMetadataResponse response = bundleLookupUseCase.getBundleMetadata(bundleHash);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/bundles/latest")
    public ResponseEntity<LatestBundleResponse> getLatestBundle(
            @RequestParam String tenantId,
            @RequestParam String subjectType,
            @RequestParam String subjectKey) {
        try {
            LatestBundleResponse response = bundleLookupUseCase.getLatestBundle(tenantId, subjectType, subjectKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/bundles:warmup")
    public ResponseEntity<Void> warmupBundles(@Valid @RequestBody WarmupRequest request) {
        try {
            bundleLookupUseCase.warmupBundles(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).build(); // Service unavailable
        }
    }

    @GetMapping(value = "/bundles/{bundleHash}/drl", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getDrlContent(@PathVariable String bundleHash) {
        try {
            String drlContent = bundleLookupUseCase.getDrlContent(bundleHash);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(drlContent);
        } catch (BundleNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(204).build(); // No content - bundle exists but no DRL stored
        }
    }
}