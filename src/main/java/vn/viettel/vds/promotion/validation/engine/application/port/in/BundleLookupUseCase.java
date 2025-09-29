package vn.viettel.vds.promotion.validation.engine.application.port.in;

import vn.viettel.vds.promotion.validation.engine.application.dto.BundleMetadataResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.LatestBundleResponse;
import vn.viettel.vds.promotion.validation.engine.application.dto.WarmupRequest;

public interface BundleLookupUseCase {

    BundleMetadataResponse getBundleMetadata(String bundleHash);

    LatestBundleResponse getLatestBundle(String tenantId, String subjectType, String subjectKey);

    void warmupBundles(WarmupRequest request);
}