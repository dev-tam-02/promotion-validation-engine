package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class ValidateOrderRequest {
    
    @NotNull
    @Valid
    private CustomerDto customer;
    
    @NotNull
    @Valid
    private OrderDto order;
    
    @NotNull
    @Valid
    private ExecutionContextDto executionContext;
    
    // Campaign filters
    private CampaignFilter campaignFilter;
    
    // Validation options
    private ValidationOptions options;

    public static class CampaignFilter {
        private List<String> campaignIds;        // Specific campaigns
        private List<String> campaignTypes;     // voucher, tier, loyalty
        private List<String> tags;              // campaign tags
        private Instant effectiveTime;          // Check at specific time (default: now)
        private Boolean activeOnly = true;      // Only active campaigns
        private Integer maxCampaigns = 50;      // Limit results
        
        // Getters and setters...
        public List<String> getCampaignIds() { return campaignIds; }
        public void setCampaignIds(List<String> campaignIds) { this.campaignIds = campaignIds; }
        
        public List<String> getCampaignTypes() { return campaignTypes; }
        public void setCampaignTypes(List<String> campaignTypes) { this.campaignTypes = campaignTypes; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        public Instant getEffectiveTime() { return effectiveTime; }
        public void setEffectiveTime(Instant effectiveTime) { this.effectiveTime = effectiveTime; }
        
        public Boolean getActiveOnly() { return activeOnly; }
        public void setActiveOnly(Boolean activeOnly) { this.activeOnly = activeOnly; }
        
        public Integer getMaxCampaigns() { return maxCampaigns; }
        public void setMaxCampaigns(Integer maxCampaigns) { this.maxCampaigns = maxCampaigns; }
    }
    
    public static class ValidationOptions {
        private Boolean explainResults = false;
        private Boolean includeInactiveCampaigns = false;
        private Boolean parallelExecution = true;
        private Integer timeoutMs = 30000;
        
        // Getters and setters...
        public Boolean getExplainResults() { return explainResults; }
        public void setExplainResults(Boolean explainResults) { this.explainResults = explainResults; }
        
        public Boolean getIncludeInactiveCampaigns() { return includeInactiveCampaigns; }
        public void setIncludeInactiveCampaigns(Boolean includeInactiveCampaigns) { this.includeInactiveCampaigns = includeInactiveCampaigns; }
        
        public Boolean getParallelExecution() { return parallelExecution; }
        public void setParallelExecution(Boolean parallelExecution) { this.parallelExecution = parallelExecution; }
        
        public Integer getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    // Getters and setters
    public CustomerDto getCustomer() { return customer; }
    public void setCustomer(CustomerDto customer) { this.customer = customer; }
    
    public OrderDto getOrder() { return order; }
    public void setOrder(OrderDto order) { this.order = order; }
    
    public ExecutionContextDto getExecutionContext() { return executionContext; }
    public void setExecutionContext(ExecutionContextDto executionContext) { this.executionContext = executionContext; }
    
    public CampaignFilter getCampaignFilter() { return campaignFilter; }
    public void setCampaignFilter(CampaignFilter campaignFilter) { this.campaignFilter = campaignFilter; }
    
    public ValidationOptions getOptions() { return options; }
    public void setOptions(ValidationOptions options) { this.options = options; }
}
