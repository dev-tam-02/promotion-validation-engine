package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import java.util.List;

public class ValidateOrderResponse {
    
    private Boolean success;
    private String message;
    private List<CampaignValidationResult> results;
    private ValidationSummary summary;
    
    public static class CampaignValidationResult {
        private String campaignId;
        private String campaignName;
        private String campaignType;
        private String bundleHash;
        private Boolean valid;
        private String decision;
        private List<String> reasonCodes;
        private List<String> explain;
        private ExecutionInfo execution;
        
        public static class ExecutionInfo {
            private String version;
            private Integer latencyMs;
            private Boolean cacheHit;
            
            // Getters and setters...
            public String getVersion() { return version; }
            public void setVersion(String version) { this.version = version; }
            
            public Integer getLatencyMs() { return latencyMs; }
            public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
            
            public Boolean getCacheHit() { return cacheHit; }
            public void setCacheHit(Boolean cacheHit) { this.cacheHit = cacheHit; }
        }
        
        // Getters and setters...
        public String getCampaignId() { return campaignId; }
        public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
        
        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
        
        public String getCampaignType() { return campaignType; }
        public void setCampaignType(String campaignType) { this.campaignType = campaignType; }
        
        public String getBundleHash() { return bundleHash; }
        public void setBundleHash(String bundleHash) { this.bundleHash = bundleHash; }
        
        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
        
        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        
        public List<String> getReasonCodes() { return reasonCodes; }
        public void setReasonCodes(List<String> reasonCodes) { this.reasonCodes = reasonCodes; }
        
        public List<String> getExplain() { return explain; }
        public void setExplain(List<String> explain) { this.explain = explain; }
        
        public ExecutionInfo getExecution() { return execution; }
        public void setExecution(ExecutionInfo execution) { this.execution = execution; }
    }
    
    public static class ValidationSummary {
        private Integer totalCampaigns;
        private Integer validCampaigns;
        private Integer invalidCampaigns;
        private Integer totalLatencyMs;
        private Integer cacheHitCount;
        
        // Getters and setters...
        public Integer getTotalCampaigns() { return totalCampaigns; }
        public void setTotalCampaigns(Integer totalCampaigns) { this.totalCampaigns = totalCampaigns; }
        
        public Integer getValidCampaigns() { return validCampaigns; }
        public void setValidCampaigns(Integer validCampaigns) { this.validCampaigns = validCampaigns; }
        
        public Integer getInvalidCampaigns() { return invalidCampaigns; }
        public void setInvalidCampaigns(Integer invalidCampaigns) { this.invalidCampaigns = invalidCampaigns; }
        
        public Integer getTotalLatencyMs() { return totalLatencyMs; }
        public void setTotalLatencyMs(Integer totalLatencyMs) { this.totalLatencyMs = totalLatencyMs; }
        
        public Integer getCacheHitCount() { return cacheHitCount; }
        public void setCacheHitCount(Integer cacheHitCount) { this.cacheHitCount = cacheHitCount; }
    }
    
    // Getters and setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public List<CampaignValidationResult> getResults() { return results; }
    public void setResults(List<CampaignValidationResult> results) { this.results = results; }
    
    public ValidationSummary getSummary() { return summary; }
    public void setSummary(ValidationSummary summary) { this.summary = summary; }
}
