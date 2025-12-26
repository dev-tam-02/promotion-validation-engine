package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Simplified request DTO for fast check evaluation.
 * Contains only the essential fields needed for quick rule checks.
 */
@Schema(description = "Fast check request for quick rule evaluation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FastCheckRequest {

    @Schema(description = "Tenant identifier", example = "viettel", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Tenant ID is required")
    @JsonProperty("tenantId")
    private String tenantId;

    @Schema(description = "Campaign/Bundle identifier", example = "tet-2025", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Campaign ID is required")
    @JsonProperty("campaignId")
    private String campaignId;

    @Schema(description = "Customer identifier", example = "CUST123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Customer ID is required")
    @JsonProperty("customerId")
    private String customerId;

    @Schema(description = "Customer segments", example = "[\"VIP\", \"GOLD\"]")
    @JsonProperty("customerSegments")
    private List<String> customerSegments;

    @Schema(description = "Customer region", example = "HCM")
    @JsonProperty("customerRegion")
    private String customerRegion;

    @Schema(description = "Order total amount", example = "500000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Order total is required")
    @JsonProperty("orderTotal")
    private Long orderTotal;

    @Schema(description = "Currency code", example = "VND", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Currency is required")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Number of items in order", example = "5")
    @JsonProperty("itemCount")
    private Integer itemCount;

    @Schema(description = "Product categories in order", example = "[\"ELECTRONICS\", \"FASHION\"]")
    @JsonProperty("orderCategories")
    private List<String> orderCategories;

    @Schema(description = "Evaluation timestamp", example = "2025-01-15T10:30:00+00:00")
    @JsonProperty("evaluationTime")
    private OffsetDateTime evaluationTime;

    @Schema(description = "Timezone for evaluation", example = "Asia/Ho_Chi_Minh")
    @JsonProperty("timezone")
    private String timezone;

    // Constructors
    public FastCheckRequest() {
    }

    public FastCheckRequest(String tenantId, String campaignId, String customerId, Long orderTotal, String currency) {
        this.tenantId = tenantId;
        this.campaignId = campaignId;
        this.customerId = customerId;
        this.orderTotal = orderTotal;
        this.currency = currency;
    }

    // Getters and setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<String> getCustomerSegments() {
        return customerSegments;
    }

    public void setCustomerSegments(List<String> customerSegments) {
        this.customerSegments = customerSegments;
    }

    public String getCustomerRegion() {
        return customerRegion;
    }

    public void setCustomerRegion(String customerRegion) {
        this.customerRegion = customerRegion;
    }

    public Long getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(Long orderTotal) {
        this.orderTotal = orderTotal;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public List<String> getOrderCategories() {
        return orderCategories;
    }

    public void setOrderCategories(List<String> orderCategories) {
        this.orderCategories = orderCategories;
    }

    public OffsetDateTime getEvaluationTime() {
        return evaluationTime;
    }

    public void setEvaluationTime(OffsetDateTime evaluationTime) {
        this.evaluationTime = evaluationTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}