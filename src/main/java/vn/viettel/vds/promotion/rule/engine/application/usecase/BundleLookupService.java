package vn.viettel.vds.promotion.rule.engine.application.usecase;

import com.promix.platform.core.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.BundleMetadataResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.LatestBundleResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.WarmupRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.in.BundleLookupUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.AssignmentRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.util.List;
import java.util.Optional;

@Service
public class BundleLookupService implements BundleLookupUseCase {

    private static final Logger logger = LoggerFactory.getLogger(BundleLookupService.class);

    private final BundleRepositoryPort bundleRepositoryPort;
    private final AssignmentRepositoryPort assignmentRepositoryPort;
    private final RuleEnginePort ruleEnginePort;

    public BundleLookupService(BundleRepositoryPort bundleRepositoryPort,
                               AssignmentRepositoryPort assignmentRepositoryPort,
                               @Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort) {
        this.bundleRepositoryPort = bundleRepositoryPort;
        this.assignmentRepositoryPort = assignmentRepositoryPort;
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
            throw new ResourceNotFoundException("NO_RULE_CONFIGURED",
                    "No bundle found for ruleId: " + ruleId);
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

        // First: lookup via assignment table (primary path)
        List<AssignmentEntity> assignments = assignmentRepositoryPort.findActiveBySubjectOrderByPriority(
                subjectType, subjectKey);

        for (AssignmentEntity assignment : assignments) {
            if (assignment.getBundleHash() != null) {
                Optional<BundleEntity> bundleOpt = bundleRepositoryPort.findById(assignment.getBundleHash());
                if (bundleOpt.isPresent()) {
                    BundleEntity bundle = bundleOpt.get();
                    if (!ruleEnginePort.isRuleBundleLoaded(bundle.getId())) {
                        logger.info("Bundle not loaded, warming up: {}", bundle.getId());
                        warmupSingleBundle(bundle.getId());
                    }
                    LatestBundleResponse response = mapToLatestBundleResponse(bundle);
                    response.setAssignmentVersion(assignment.getSourceVersion() != null
                            ? assignment.getSourceVersion().intValue() : 0);
                    return response;
                }
            }

            // Assignment exists but bundle not found: try by ruleId
            if (assignment.getRuleId() != null) {
                Optional<BundleEntity> byRule = bundleRepositoryPort.findActiveBundles().stream()
                        .filter(b -> assignment.getRuleId().equals(b.getRuleId()))
                        .max((b1, b2) -> b1.getRuleVersion().compareTo(b2.getRuleVersion()));
                if (byRule.isPresent()) {
                    BundleEntity bundle = byRule.get();
                    if (!ruleEnginePort.isRuleBundleLoaded(bundle.getId())) {
                        warmupSingleBundle(bundle.getId());
                    }
                    return mapToLatestBundleResponse(bundle);
                }
            }
        }

        // Fail-closed: no assignment or bundle → deny caller with
        // NO_RULE_CONFIGURED so pp-redemption never silently allows a
        // redemption when no rule is present.
        logger.warn("No active assignment or bundle found for subject: {}/{}", subjectType, subjectKey);
        throw new ResourceNotFoundException("NO_RULE_CONFIGURED",
                "No active rule configured for " + subjectType + ":" + subjectKey);
    }

    @Override
    public BundleMetadataResponse getBundleMetadata(String bundleHash) {
        logger.info("Getting metadata for bundle: {}", bundleHash);

        Optional<BundleEntity> bundleOpt = bundleRepositoryPort.findById(bundleHash);
        if (bundleOpt.isEmpty()) {
            throw new ResourceNotFoundException("BUNDLE_NOT_FOUND",
                    "Bundle not found: " + bundleHash);
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
            throw new ResourceNotFoundException("BUNDLE_NOT_FOUND",
                    "Bundle not found: " + bundleHash);
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
