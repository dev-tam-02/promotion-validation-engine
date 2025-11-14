package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.viettel.vds.promotion.validation.engine.application.dto.BundleMetadataResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.LatestBundleResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.WarmupRequest;
import vn.viettel.vds.promotion.validation.engine.application.port.in.BundleLookupUseCase;
import vn.viettel.vds.promotion.validation.engine.domain.exception.BundleNotFoundException;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1")
public class BundleController {

    private final BundleLookupUseCase bundleLookupUseCase;

    public BundleController(BundleLookupUseCase bundleLookupUseCase) {
        this.bundleLookupUseCase = bundleLookupUseCase;
    }

    @GetMapping("/bundles/{bundleHash}")
    public BundleMetadataResponse getBundleMetadata(@PathVariable String bundleHash) {
        return bundleLookupUseCase.getBundleMetadata(bundleHash);
    }

    @GetMapping("/bundles/latest")
    public LatestBundleResponse getLatestBundle(
            @RequestParam String tenantId,
            @RequestParam String subjectType,
            @RequestParam String subjectKey) {
            return bundleLookupUseCase.getLatestBundle(tenantId, subjectType, subjectKey);
    }

    @PostMapping("/bundles:warmup")
    public void warmupBundles(@Valid @RequestBody WarmupRequest request) {
            bundleLookupUseCase.warmupBundles(request);
    }

    @GetMapping(value = "/bundles/{bundleHash}/drl", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getDrlContent(@PathVariable String bundleHash) {
           return bundleLookupUseCase.getDrlContent(bundleHash);
    }
}