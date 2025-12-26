package vn.viettel.vds.promotion.rule.engine.domain.service.bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.repository.BundleJpaRepository;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ObjectStoragePort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class BundleRepository {

    private static final Logger logger = LoggerFactory.getLogger(BundleRepository.class);

    private final BundleJpaRepository bundleJpaRepository;
    private final ObjectStoragePort objectStoragePort;

    public BundleRepository(BundleJpaRepository bundleJpaRepository,
                            ObjectStoragePort objectStoragePort) {
        this.bundleJpaRepository = bundleJpaRepository;
        this.objectStoragePort = objectStoragePort;
    }

    public List<ActiveRuleInfo> findActiveRules() {
        logger.debug("Finding all active rules from database");

        List<BundleEntity> bundles = bundleJpaRepository.findAll();
        logger.debug("Found {} bundles in database", bundles.size());

        List<ActiveRuleInfo> activeRules = new ArrayList<>();

        for (BundleEntity bundle : bundles) {
            try {
                ActiveRuleInfo ruleInfo = buildActiveRuleInfo(bundle);
                if (ruleInfo != null) {
                    activeRules.add(ruleInfo);
                }
            } catch (Exception e) {
                logger.warn("Failed to build ActiveRuleInfo for bundle: {}", bundle.getId(), e);
            }
        }

        logger.info("Found {} active rules ready for preload", activeRules.size());
        return activeRules;
    }

    public ActiveRuleInfo findActiveRule(String ruleId) {
        logger.debug("Finding active rule by ruleId: {}", ruleId);

        List<BundleEntity> bundles = bundleJpaRepository.findAll();

        for (BundleEntity bundle : bundles) {
            if (bundle.getRuleId().equals(ruleId)) {
                return buildActiveRuleInfo(bundle);
            }
        }

        logger.warn("No active rule found for ruleId: {}", ruleId);
        return null;
    }

    private ActiveRuleInfo buildActiveRuleInfo(BundleEntity bundle) {
        if (bundle.getArtifact() == null || bundle.getArtifact().getKey() == null) {
            logger.warn("Bundle {} has no artifact information", bundle.getId());
            return null;
        }

        String artifactKey = bundle.getArtifact().getKey();
        Optional<byte[]> artifactBytes = objectStoragePort.retrieve(artifactKey);

        if (artifactBytes.isEmpty()) {
            logger.warn("Failed to retrieve artifact for bundle {}: key={}", bundle.getId(), artifactKey);
            return null;
        }

        logger.debug("Successfully retrieved artifact for bundle {}: size={} bytes",
                bundle.getId(), artifactBytes.get().length);

        return new ActiveRuleInfo(
                bundle.getRuleId(),
                bundle.getId(), // bundleHash
                artifactBytes.get(),
                bundle.getTenantId(),
                bundle.getRuleVersion(),
                bundle.getCreatedAt()
        );
    }
}