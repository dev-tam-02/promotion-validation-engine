package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;

import java.time.Instant;
import java.util.List;

@Service
public class CampaignDiscoveryService {

    private final BundleRepositoryPort bundleRepositoryPort;

    public CampaignDiscoveryService(BundleRepositoryPort bundleRepositoryPort) {
        this.bundleRepositoryPort = bundleRepositoryPort;
    }

    public List<OrderValidationService.CampaignInfo> discoverCampaigns(DiscoveryRequest request) {
        // For now, discover from bundle repository
        // In production, this should integrate with campaign service

        return bundleRepositoryPort.findActiveBundles()
                .stream()
                .map(bundle -> {
                    OrderValidationService.CampaignInfo info = new OrderValidationService.CampaignInfo();
                    info.setCampaignId(bundle.getRuleId()); // Using ruleId as campaignId for now
                    info.setCampaignName(bundle.getRuleId());
                    info.setCampaignType("voucher"); // Default type
                    info.setBundleHash(bundle.getId());
                    return info;
                })
                .limit(request.getMaxResults())
                .toList();
    }

    public static class DiscoveryRequest {
        private List<String> campaignIds;
        private List<String> campaignTypes;
        private List<String> tags;
        private Instant effectiveTime;
        private Boolean activeOnly;
        private Integer maxResults;

        // Getters and setters...
        public List<String> getCampaignIds() {
            return campaignIds;
        }

        public void setCampaignIds(List<String> campaignIds) {
            this.campaignIds = campaignIds;
        }

        public List<String> getCampaignTypes() {
            return campaignTypes;
        }

        public void setCampaignTypes(List<String> campaignTypes) {
            this.campaignTypes = campaignTypes;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public Instant getEffectiveTime() {
            return effectiveTime;
        }

        public void setEffectiveTime(Instant effectiveTime) {
            this.effectiveTime = effectiveTime;
        }

        public Boolean getActiveOnly() {
            return activeOnly;
        }

        public void setActiveOnly(Boolean activeOnly) {
            this.activeOnly = activeOnly;
        }

        public Integer getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(Integer maxResults) {
            this.maxResults = maxResults;
        }
    }
}
