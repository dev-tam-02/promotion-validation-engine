package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Redemption context for validation rules.
 * Contains information about the current redemption request.
 */
public class Redemption {
    private boolean codeHolder;
    private int perCustomerPerDay;
    private int perCustomerTotal;
    private boolean redeemingCodeHolder;
    private String locationId;

    // Extended fields for redemption operators
    private String userId;
    private String apiKey;
    private Map<String, Object> metadata;

    public Redemption() {
        this.metadata = new HashMap<>();
    }

    public Redemption(boolean redeemingCodeHolder, String locationId) {
        this();
        this.redeemingCodeHolder = redeemingCodeHolder;
        this.locationId = locationId;
    }

    public boolean isRedeemingCodeHolder() {
        return redeemingCodeHolder;
    }

    public void setRedeemingCodeHolder(boolean redeemingCodeHolder) {
        this.redeemingCodeHolder = redeemingCodeHolder;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public boolean isCodeHolder() {
        return codeHolder;
    }

    public void setCodeHolder(boolean codeHolder) {
        this.codeHolder = codeHolder;
    }

    public int getPerCustomerPerDay() {
        return perCustomerPerDay;
    }

    public void setPerCustomerPerDay(int perCustomerPerDay) {
        this.perCustomerPerDay = perCustomerPerDay;
    }

    public int getPerCustomerTotal() {
        return perCustomerTotal;
    }

    public void setPerCustomerTotal(int perCustomerTotal) {
        this.perCustomerTotal = perCustomerTotal;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    /**
     * Get metadata value by key.
     * Used in DRL rules for metadata checks.
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
}