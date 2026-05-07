package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.BundleEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RuleDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(RuleDiscoveryService.class);

    private final BundleRepositoryPort bundleRepositoryPort;
    private final RuleEnginePort ruleEnginePort;

    public RuleDiscoveryService(BundleRepositoryPort bundleRepositoryPort,
                                RuleEnginePort ruleEnginePort) {
        this.bundleRepositoryPort = bundleRepositoryPort;
        this.ruleEnginePort = ruleEnginePort;
    }

    /**
     * Discover and validate all active rules
     * Returns list of valid campaignIds
     */
    public List<String> discoverValidCampaigns(DiscoveryRequest request) {
        logger.info("Discovering valid campaigns");

        // 1. Get all active bundles
        List<BundleEntity> activeBundles = bundleRepositoryPort.findActiveBundles();

        if (activeBundles.isEmpty()) {
            logger.info("No active bundles found");
            return List.of();
        }

        logger.info("Found {} active bundles", activeBundles.size());

        // 2. Execute validation for all bundles in batch
        List<RuleEnginePort.ExecuteInput> executeInputs = activeBundles.stream()
                .map(bundle -> new RuleEnginePort.ExecuteInput(
                        bundle.getId(), // bundleHash
                        request.getContext(),
                        new RuleEnginePort.ExecuteOptions("NONE", 30000, 1000)
                ))
                .toList();

        List<ExecuteResponse> responses = ruleEnginePort.executeBatch(executeInputs);

        // 3. Filter valid campaigns
        List<String> validCampaigns = new ArrayList<>();
        for (int i = 0; i < activeBundles.size(); i++) {
            BundleEntity bundle = activeBundles.get(i);
            ExecuteResponse response = responses.get(i);

            if (Boolean.TRUE.equals(response.getOk()) && "ALLOW".equals(response.getDecision())) {
                validCampaigns.add(bundle.getRuleId()); // Using ruleId as campaignId
                logger.debug("Campaign {} is valid for this context", bundle.getRuleId());
            } else {
                logger.debug("Campaign {} is invalid: decision={}, reasons={}",
                        bundle.getRuleId(), response.getDecision(), response.getReasonCodes());
            }
        }

        logger.info("Found {} valid campaigns out of {} total",
                validCampaigns.size(), activeBundles.size());

        return validCampaigns;
    }

    /**
     * Discover and return detailed validation results
     */
    public List<CampaignValidationResult> discoverWithDetails(DiscoveryRequest request) {
        logger.info("Discovering campaigns with details");

        List<BundleEntity> activeBundles = bundleRepositoryPort.findActiveBundles();

        if (activeBundles.isEmpty()) {
            return List.of();
        }

        // Execute validation for all bundles
        List<RuleEnginePort.ExecuteInput> executeInputs = activeBundles.stream()
                .map(bundle -> new RuleEnginePort.ExecuteInput(
                        bundle.getId(),
                        request.getContext(),
                        new RuleEnginePort.ExecuteOptions(
                                request.isExplainResults() ? "FULL" : "NONE",
                                30000, 1000
                        )
                ))
                .toList();

        List<ExecuteResponse> responses = ruleEnginePort.executeBatch(executeInputs);

        // Map to detailed results
        List<CampaignValidationResult> results = new ArrayList<>();
        for (int i = 0; i < activeBundles.size(); i++) {
            BundleEntity bundle = activeBundles.get(i);
            ExecuteResponse response = responses.get(i);

            CampaignValidationResult result = new CampaignValidationResult();
            result.setCampaignId(bundle.getRuleId());
            result.setBundleHash(bundle.getId());
            result.setValid(Boolean.TRUE.equals(response.getOk()) && "ALLOW".equals(response.getDecision()));
            result.setDecision(response.getDecision());
            result.setReasonCodes(response.getReasonCodes());
            result.setLatencyMs(response.getEngine().getLatencyMs());
            result.setCacheHit(response.getEngine().getCacheHit());

            if (request.isExplainResults()) {
                result.setExplain(response.getExplain().stream()
                        .map(entry -> entry.getNode() + ": " + entry.getOperator() + " = " + entry.getResult())
                        .collect(Collectors.toList()));
            }

            results.add(result);
        }

        return results;
    }

    public static class DiscoveryRequest {
        private Map<String, Object> context;
        private boolean explainResults = false;

        public DiscoveryRequest(Map<String, Object> context) {
            this.context = context;
        }

        // Getters and setters
        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }

        public boolean isExplainResults() {
            return explainResults;
        }

        public void setExplainResults(boolean explainResults) {
            this.explainResults = explainResults;
        }
    }

    public static class CampaignValidationResult {
        private String campaignId;
        private String bundleHash;
        private boolean valid;
        private String decision;
        private List<String> reasonCodes;
        private List<String> explain;
        private Integer latencyMs;
        private Boolean cacheHit;

        // Getters and setters
        public String getCampaignId() {
            return campaignId;
        }

        public void setCampaignId(String campaignId) {
            this.campaignId = campaignId;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public void setBundleHash(String bundleHash) {
            this.bundleHash = bundleHash;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getDecision() {
            return decision;
        }

        public void setDecision(String decision) {
            this.decision = decision;
        }

        public List<String> getReasonCodes() {
            return reasonCodes;
        }

        public void setReasonCodes(List<String> reasonCodes) {
            this.reasonCodes = reasonCodes;
        }

        public List<String> getExplain() {
            return explain;
        }

        public void setExplain(List<String> explain) {
            this.explain = explain;
        }

        public Integer getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(Integer latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Boolean getCacheHit() {
            return cacheHit;
        }

        public void setCacheHit(Boolean cacheHit) {
            this.cacheHit = cacheHit;
        }
    }
}
