package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.ValidateOrderRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.ValidateOrderResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class OrderValidationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderValidationService.class);

    private final RuleEnginePort ruleEnginePort;
    private final CampaignDiscoveryService campaignDiscoveryService;
    private final Executor validationExecutor;

    public OrderValidationService(RuleEnginePort ruleEnginePort,
                                  CampaignDiscoveryService campaignDiscoveryService) {
        this.ruleEnginePort = ruleEnginePort;
        this.campaignDiscoveryService = campaignDiscoveryService;
        this.validationExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public ValidateOrderResponse validateOrder(ValidateOrderRequest request) {
        logger.info("Starting order validation: customerId={}, orderId={}", 
                request.getCustomer().id(), request.getOrder().id());

        long startTime = System.currentTimeMillis();

        try {
            // 1. Discover applicable campaigns
            List<CampaignInfo> campaigns = discoverCampaigns(request);
            
            if (campaigns.isEmpty()) {
                return buildEmptyResponse("No applicable campaigns found");
            }

            // 2. Execute validation for all campaigns
            List<ValidateOrderResponse.CampaignValidationResult> results = 
                    executeValidations(request, campaigns);

            // 3. Build response with summary
            return buildSuccessResponse(results, startTime);

        } catch (Exception e) {
            logger.error("Order validation failed", e);
            return buildErrorResponse(e.getMessage());
        }
    }

    private List<CampaignInfo> discoverCampaigns(ValidateOrderRequest request) {
        ValidateOrderRequest.CampaignFilter filter = request.getCampaignFilter();
        
        CampaignDiscoveryService.DiscoveryRequest discoveryRequest = 
                new CampaignDiscoveryService.DiscoveryRequest();
        
        discoveryRequest.setTenantId(request.getExecutionContext().getTenantId());
        discoveryRequest.setEffectiveTime(filter != null ? filter.getEffectiveTime() : Instant.now());
        discoveryRequest.setActiveOnly(filter != null ? filter.getActiveOnly() : true);
        discoveryRequest.setMaxResults(filter != null ? filter.getMaxCampaigns() : 50);
        
        if (filter != null) {
            discoveryRequest.setCampaignIds(filter.getCampaignIds());
            discoveryRequest.setCampaignTypes(filter.getCampaignTypes());
            discoveryRequest.setTags(filter.getTags());
        }

        return campaignDiscoveryService.discoverCampaigns(discoveryRequest);
    }

    private List<ValidateOrderResponse.CampaignValidationResult> executeValidations(
            ValidateOrderRequest request, List<CampaignInfo> campaigns) {

        // Prepare context for rule execution
        Map<String, Object> context = buildExecutionContext(request);

        // Create execution inputs for all campaigns
        List<RuleEnginePort.ExecuteInput> executeInputs = campaigns.stream()
                .map(campaign -> new RuleEnginePort.ExecuteInput(
                        request.getExecutionContext().getTenantId(),
                        campaign.getBundleHash(),
                        context,
                        buildExecuteOptions(request.getOptions())
                ))
                .toList();

        // Execute in batch for better performance
        List<vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse> executeResponses = 
                ruleEnginePort.executeBatch(executeInputs);

        // Map results back to campaigns
        List<ValidateOrderResponse.CampaignValidationResult> results = new ArrayList<>();
        for (int i = 0; i < campaigns.size(); i++) {
            CampaignInfo campaign = campaigns.get(i);
            vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse executeResponse = 
                    executeResponses.get(i);
            
            results.add(mapToValidationResult(campaign, executeResponse));
        }

        return results;
    }

    private Map<String, Object> buildExecutionContext(ValidateOrderRequest request) {
        Map<String, Object> context = new HashMap<>();
        context.put("customer", request.getCustomer());
        context.put("order", request.getOrder());
        context.put("executionContext", request.getExecutionContext());
        return context;
    }

    private RuleEnginePort.ExecuteOptions buildExecuteOptions(ValidateOrderRequest.ValidationOptions options) {
        if (options == null) {
            return new RuleEnginePort.ExecuteOptions("NONE", 30000, 1000);
        }
        
        String explainLevel = Boolean.TRUE.equals(options.getExplainResults()) ? "FULL" : "NONE";
        return new RuleEnginePort.ExecuteOptions(
                explainLevel, 
                options.getTimeoutMs(), 
                1000
        );
    }

    private ValidateOrderResponse.CampaignValidationResult mapToValidationResult(
            CampaignInfo campaign, 
            vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse executeResponse) {
        
        ValidateOrderResponse.CampaignValidationResult result = 
                new ValidateOrderResponse.CampaignValidationResult();
        
        result.setCampaignId(campaign.getCampaignId());
        result.setCampaignName(campaign.getCampaignName());
        result.setCampaignType(campaign.getCampaignType());
        result.setBundleHash(campaign.getBundleHash());
        result.setValid(executeResponse.getOk());
        result.setDecision(executeResponse.getDecision());
        result.setReasonCodes(executeResponse.getReasonCodes());
        
        // Convert explain entries to strings
        List<String> explainStrings = executeResponse.getExplain().stream()
                .map(entry -> entry.getNode() + ": " + entry.getOperator() + " = " + entry.getResult())
                .toList();
        result.setExplain(explainStrings);
        
        // Map execution info
        ValidateOrderResponse.CampaignValidationResult.ExecutionInfo executionInfo = 
                new ValidateOrderResponse.CampaignValidationResult.ExecutionInfo();
        executionInfo.setVersion(executeResponse.getEngine().getVersion());
        executionInfo.setLatencyMs(executeResponse.getEngine().getLatencyMs());
        executionInfo.setCacheHit(executeResponse.getEngine().getCacheHit());
        result.setExecution(executionInfo);
        
        return result;
    }

    private ValidateOrderResponse buildSuccessResponse(
            List<ValidateOrderResponse.CampaignValidationResult> results, long startTime) {
        
        ValidateOrderResponse response = new ValidateOrderResponse();
        response.setSuccess(true);
        response.setResults(results);
        
        // Build summary
        ValidateOrderResponse.ValidationSummary summary = new ValidateOrderResponse.ValidationSummary();
        summary.setTotalCampaigns(results.size());
        summary.setValidCampaigns((int) results.stream().filter(r -> Boolean.TRUE.equals(r.getValid())).count());
        summary.setInvalidCampaigns(results.size() - summary.getValidCampaigns());
        summary.setTotalLatencyMs((int) (System.currentTimeMillis() - startTime));
        summary.setCacheHitCount((int) results.stream()
                .filter(r -> Boolean.TRUE.equals(r.getExecution().getCacheHit())).count());
        
        response.setSummary(summary);
        return response;
    }

    private ValidateOrderResponse buildEmptyResponse(String message) {
        ValidateOrderResponse response = new ValidateOrderResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setResults(List.of());
        
        ValidateOrderResponse.ValidationSummary summary = new ValidateOrderResponse.ValidationSummary();
        summary.setTotalCampaigns(0);
        summary.setValidCampaigns(0);
        summary.setInvalidCampaigns(0);
        summary.setTotalLatencyMs(0);
        summary.setCacheHitCount(0);
        response.setSummary(summary);
        
        return response;
    }

    private ValidateOrderResponse buildErrorResponse(String errorMessage) {
        ValidateOrderResponse response = new ValidateOrderResponse();
        response.setSuccess(false);
        response.setMessage(errorMessage);
        response.setResults(List.of());
        return response;
    }

    // Campaign info class
    public static class CampaignInfo {
        private String campaignId;
        private String campaignName;
        private String campaignType;
        private String bundleHash;
        
        // Getters and setters...
        public String getCampaignId() { return campaignId; }
        public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
        
        public String getCampaignName() { return campaignName; }
        public void setCampaignName(String campaignName) { this.campaignName = campaignName; }
        
        public String getCampaignType() { return campaignType; }
        public void setCampaignType(String campaignType) { this.campaignType = campaignType; }
        
        public String getBundleHash() { return bundleHash; }
        public void setBundleHash(String bundleHash) { this.bundleHash = bundleHash; }
    }
}
