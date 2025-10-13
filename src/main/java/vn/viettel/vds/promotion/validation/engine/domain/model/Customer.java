package vn.viettel.vds.promotion.validation.engine.domain.model;

import java.util.Map;
import java.util.Set;

public class Customer {
    private String id;
    private Set<String> segments;
    private Map<String, Object> attrs;
    private String loyaltyTier;
    private boolean redeemingCodeHolder;
    private double totalOrdersValue;
    private double totalDiscountedAmount;
    private int redemptionsPerDay;
    private int redemptionsPerIncentive;
    private String acquisitionChannel;
    private int loyaltyPoints;

    public Customer() {
    }

    public Customer(String id, Set<String> segments) {
        this.id = id;
        this.segments = segments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<String> getSegments() {
        return segments;
    }

    public void setSegments(Set<String> segments) {
        this.segments = segments;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }

    public String getLoyaltyTier() {
        return loyaltyTier;
    }

    public void setLoyaltyTier(String loyaltyTier) {
        this.loyaltyTier = loyaltyTier;
    }

    public boolean isRedeemingCodeHolder() {
        return redeemingCodeHolder;
    }

    public void setRedeemingCodeHolder(boolean redeemingCodeHolder) {
        this.redeemingCodeHolder = redeemingCodeHolder;
    }

    public double getTotalOrdersValue() {
        return totalOrdersValue;
    }

    public void setTotalOrdersValue(double totalOrdersValue) {
        this.totalOrdersValue = totalOrdersValue;
    }

    public double getTotalDiscountedAmount() {
        return totalDiscountedAmount;
    }

    public void setTotalDiscountedAmount(double totalDiscountedAmount) {
        this.totalDiscountedAmount = totalDiscountedAmount;
    }

    public int getRedemptionsPerDay() {
        return redemptionsPerDay;
    }

    public void setRedemptionsPerDay(int redemptionsPerDay) {
        this.redemptionsPerDay = redemptionsPerDay;
    }

    public int getRedemptionsPerIncentive() {
        return redemptionsPerIncentive;
    }

    public void setRedemptionsPerIncentive(int redemptionsPerIncentive) {
        this.redemptionsPerIncentive = redemptionsPerIncentive;
    }

    public String getAcquisitionChannel() {
        return acquisitionChannel;
    }

    public void setAcquisitionChannel(String acquisitionChannel) {
        this.acquisitionChannel = acquisitionChannel;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    /**
     * Alias method for tier operator compatibility
     * @return loyalty tier of the customer
     */
    public String getTier() {
        return getLoyaltyTier();
    }

    /**
     * Alias method for tier operator compatibility
     * @param tier the tier to set
     */
    public void setTier(String tier) {
        setLoyaltyTier(tier);
    }
}