package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.BundleMetadataResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.LatestBundleResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.WarmupRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.in.BundleLookupUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.util.List;
import java.util.Optional;

@Service
public class BundleLookupService implements BundleLookupUseCase {

    private static final Logger logger = LoggerFactory.getLogger(BundleLookupService.class);

    private final BundleRepositoryPort bundleRepositoryPort;
    private final RuleEnginePort ruleEnginePort;

    public BundleLookupService(BundleRepositoryPort bundleRepositoryPort,
                               @Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort) {
        this.bundleRepositoryPort = bundleRepositoryPort;
        this.ruleEnginePort = ruleEnginePort;
    }

    @Override
    public LatestBundleResponse getLatestBundle(String ruleId) {
        logger.info("Getting latest bundle for ruleId: {}", ruleId);

        List<BundleEntity> bundles = bundleRepositoryPort.findActiveBundles();
        Optional<BundleEntity> latestBundle = bundles.stream()
                .filter(b -> ruleId.equals(b.getRuleId()))
                .max((b1, b2) -> b1.getRuleVersion().compareTo(b2.getRuleVersion()));

        if (latestBundle.isEmpty()) {
            throw new IllegalArgumentException("No bundle found for ruleId: " + ruleId);
        }

        BundleEntity bundle = latestBundle.get();
        String bundleHash = bundle.getId();

        if (!ruleEnginePort.isRuleBundleLoaded(bundleHash)) {
            logger.info("Bundle not loaded, warming up: {}", bundleHash);
            warmupSingleBundle(bundleHash);
        }

        return mapToLatestBundleResponse(bundle);
    }

    @Override
    public LatestBundleResponse getLatestBundle(String subjectType, String subjectKey) {
        logger.info("Getting latest bundle for subject: {}/{}", subjectType, subjectKey);

        List<BundleEntity> bundles = bundleRepositoryPort.findActiveBundles();
        Optional<BundleEntity> latestBundle = bundles.stream()
                .max((b1, b2) -> b1.getRuleVersion().compareTo(b2.getRuleVersion()));

        if (latestBundle.isEmpty()) {
            throw new IllegalArgumentException("No bundle found for subject: " + subjectType + "/" + subjectKey);
        }

        BundleEntity bundle = latestBundle.get();
        String bundleHash = bundle.getId();

        if (!ruleEnginePort.isRuleBundleLoaded(bundleHash)) {
            logger.info("Bundle not loaded, warming up: {}", bundleHash);
            warmupSingleBundle(bundleHash);
        }

        return mapToLatestBundleResponse(bundle);
    }

    @Override
    public BundleMetadataResponse getBundleMetadata(String bundleHash) {
        logger.info("Getting metadata for bundle: {}", bundleHash);

        Optional<BundleEntity> bundleOpt = bundleRepositoryPort.findById(bundleHash);
        if (bundleOpt.isEmpty()) {
            throw new IllegalArgumentException("Bundle not found: " + bundleHash);
        }

        BundleEntity bundle = bundleOpt.get();
        return mapToBundleMetadataResponse(bundle);
    }

    @Override
    public void warmupBundles(WarmupRequest request) {
        logger.info("Warming up {} bundles", request.getBundleHashes().size());

        for (String bundleHash : request.getBundleHashes()) {
            warmupSingleBundle(bundleHash);
        }
    }

    @Override
    public String getDrlContent(String bundleHash) {
        logger.info("Getting DRL content for bundle: {}", bundleHash);

        Optional<BundleEntity> bundleOpt = bundleRepositoryPort.findById(bundleHash);
        if (bundleOpt.isEmpty()) {
            throw new IllegalArgumentException("Bundle not found: " + bundleHash);
        }

        BundleEntity bundle = bundleOpt.get();
        return bundle.getDrlContent() != null ? bundle.getDrlContent() : "";
    }

    private void warmupSingleBundle(String bundleHash) {
        try {
            logger.info("Bundle warmup requested: {}", bundleHash);
            // Simplified warmup - actual implementation would load the bundle
        } catch (Exception e) {
            logger.error("Failed to warmup bundle: {}", bundleHash, e);
        }
    }

    private LatestBundleResponse mapToLatestBundleResponse(BundleEntity bundle) {
        LatestBundleResponse response = new LatestBundleResponse();
        response.setBundleHash(bundle.getId());
        response.setRuleId(bundle.getRuleId());
        response.setRuleVersion(bundle.getRuleVersion());
        return response;
    }

    private BundleMetadataResponse mapToBundleMetadataResponse(BundleEntity bundle) {
        BundleMetadataResponse response = new BundleMetadataResponse();
        response.setRuleId(bundle.getRuleId());
        response.setRuleVersion(bundle.getRuleVersion());

        if (bundle.getEngine() != null) {
            BundleMetadataResponse.Engine engine = new BundleMetadataResponse.Engine();
            engine.setDroolsVersion(bundle.getEngine().getDroolsVersion());
            response.setEngine(engine);
        }

        return response;
    }
}
