package vn.viettel.vds.promotion.rule.engine.application.port.in;

import vn.viettel.vds.promotion.rule.engine.application.dto.BundleMetadataResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.LatestBundleResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.WarmupRequest;

public interface BundleLookupUseCase {

    LatestBundleResponse getLatestBundle(String ruleId);

    LatestBundleResponse getLatestBundle(String subjectType, String subjectKey);

    BundleMetadataResponse getBundleMetadata(String bundleHash);

    void warmupBundles(WarmupRequest request);

    String getDrlContent(String bundleHash);
}