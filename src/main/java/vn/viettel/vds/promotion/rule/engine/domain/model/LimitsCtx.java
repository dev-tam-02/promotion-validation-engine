package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Budget/Limits context for fast-check validation.
 * Contains pre-aggregated usage statistics for quick budget constraint checks.
 */
public class LimitsCtx {
    // Existing fields
    private int perCodeTotalUsed;
    private int perCustomerUsed;
    private LocalDate date;

    // Budget constraint fields - Total redemptions
    private int totalRedemptions;
    private int redemptionsPerDay;
    private int redemptionsPerMonth;

    // Budget constraint fields - Per customer
    private int perCustomerPerDay;
    private int perCustomerPerMonth;

    // Budget constraint fields - Monetary amounts
    private BigDecimal totalDiscountedAmount;
    private BigDecimal totalOrdersValue;
    private BigDecimal totalGiftAmount;
    private BigDecimal totalPayWithPoints;

    // Budget limits (configured maximums)
    private int maxTotalRedemptions;
    private int maxRedemptionsPerDay;
    private int maxRedemptionsPerMonth;
    private int maxPerCustomer;
    private int maxPerCustomerPerDay;
    private int maxPerCustomerPerMonth;
    private BigDecimal maxDiscountedAmount;
    private BigDecimal maxOrdersValue;
    private BigDecimal maxGiftAmount;
    private BigDecimal maxPayWithPoints;

    public LimitsCtx() {
        this.totalDiscountedAmount = BigDecimal.ZERO;
        this.totalOrdersValue = BigDecimal.ZERO;
        this.totalGiftAmount = BigDecimal.ZERO;
        this.totalPayWithPoints = BigDecimal.ZERO;
    }

    public LimitsCtx(int perCodeTotalUsed, int perCustomerUsed, LocalDate date) {
        this();
        this.perCodeTotalUsed = perCodeTotalUsed;
        this.perCustomerUsed = perCustomerUsed;
        this.date = date;
    }

    // Existing getters/setters
    public int getPerCodeTotalUsed() {
        return perCodeTotalUsed;
    }

    public void setPerCodeTotalUsed(int perCodeTotalUsed) {
        this.perCodeTotalUsed = perCodeTotalUsed;
    }

    public int getPerCustomerUsed() {
        return perCustomerUsed;
    }

    public void setPerCustomerUsed(int perCustomerUsed) {
        this.perCustomerUsed = perCustomerUsed;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    // Budget constraint getters/setters - Total redemptions
    public int getTotalRedemptions() {
        return totalRedemptions;
    }

    public void setTotalRedemptions(int totalRedemptions) {
        this.totalRedemptions = totalRedemptions;
    }

    public int getRedemptionsPerDay() {
        return redemptionsPerDay;
    }

    public void setRedemptionsPerDay(int redemptionsPerDay) {
        this.redemptionsPerDay = redemptionsPerDay;
    }

    public int getRedemptionsPerMonth() {
        return redemptionsPerMonth;
    }

    public void setRedemptionsPerMonth(int redemptionsPerMonth) {
        this.redemptionsPerMonth = redemptionsPerMonth;
    }

    // Budget constraint getters/setters - Per customer
    public int getPerCustomerPerDay() {
        return perCustomerPerDay;
    }

    public void setPerCustomerPerDay(int perCustomerPerDay) {
        this.perCustomerPerDay = perCustomerPerDay;
    }

    public int getPerCustomerPerMonth() {
        return perCustomerPerMonth;
    }

    public void setPerCustomerPerMonth(int perCustomerPerMonth) {
        this.perCustomerPerMonth = perCustomerPerMonth;
    }

    // Budget constraint getters/setters - Monetary amounts
    public BigDecimal getTotalDiscountedAmount() {
        return totalDiscountedAmount;
    }

    public void setTotalDiscountedAmount(BigDecimal totalDiscountedAmount) {
        this.totalDiscountedAmount = totalDiscountedAmount != null ? totalDiscountedAmount : BigDecimal.ZERO;
    }

    public BigDecimal getTotalOrdersValue() {
        return totalOrdersValue;
    }

    public void setTotalOrdersValue(BigDecimal totalOrdersValue) {
        this.totalOrdersValue = totalOrdersValue != null ? totalOrdersValue : BigDecimal.ZERO;
    }

    public BigDecimal getTotalGiftAmount() {
        return totalGiftAmount;
    }

    public void setTotalGiftAmount(BigDecimal totalGiftAmount) {
        this.totalGiftAmount = totalGiftAmount != null ? totalGiftAmount : BigDecimal.ZERO;
    }

    public BigDecimal getTotalPayWithPoints() {
        return totalPayWithPoints;
    }

    public void setTotalPayWithPoints(BigDecimal totalPayWithPoints) {
        this.totalPayWithPoints = totalPayWithPoints != null ? totalPayWithPoints : BigDecimal.ZERO;
    }

    // Budget limits getters/setters
    public int getMaxTotalRedemptions() {
        return maxTotalRedemptions;
    }

    public void setMaxTotalRedemptions(int maxTotalRedemptions) {
        this.maxTotalRedemptions = maxTotalRedemptions;
    }

    public int getMaxRedemptionsPerDay() {
        return maxRedemptionsPerDay;
    }

    public void setMaxRedemptionsPerDay(int maxRedemptionsPerDay) {
        this.maxRedemptionsPerDay = maxRedemptionsPerDay;
    }

    public int getMaxRedemptionsPerMonth() {
        return maxRedemptionsPerMonth;
    }

    public void setMaxRedemptionsPerMonth(int maxRedemptionsPerMonth) {
        this.maxRedemptionsPerMonth = maxRedemptionsPerMonth;
    }

    public int getMaxPerCustomer() {
        return maxPerCustomer;
    }

    public void setMaxPerCustomer(int maxPerCustomer) {
        this.maxPerCustomer = maxPerCustomer;
    }

    public int getMaxPerCustomerPerDay() {
        return maxPerCustomerPerDay;
    }

    public void setMaxPerCustomerPerDay(int maxPerCustomerPerDay) {
        this.maxPerCustomerPerDay = maxPerCustomerPerDay;
    }

    public int getMaxPerCustomerPerMonth() {
        return maxPerCustomerPerMonth;
    }

    public void setMaxPerCustomerPerMonth(int maxPerCustomerPerMonth) {
        this.maxPerCustomerPerMonth = maxPerCustomerPerMonth;
    }

    public BigDecimal getMaxDiscountedAmount() {
        return maxDiscountedAmount;
    }

    public void setMaxDiscountedAmount(BigDecimal maxDiscountedAmount) {
        this.maxDiscountedAmount = maxDiscountedAmount;
    }

    public BigDecimal getMaxOrdersValue() {
        return maxOrdersValue;
    }

    public void setMaxOrdersValue(BigDecimal maxOrdersValue) {
        this.maxOrdersValue = maxOrdersValue;
    }

    public BigDecimal getMaxGiftAmount() {
        return maxGiftAmount;
    }

    public void setMaxGiftAmount(BigDecimal maxGiftAmount) {
        this.maxGiftAmount = maxGiftAmount;
    }

    public BigDecimal getMaxPayWithPoints() {
        return maxPayWithPoints;
    }

    public void setMaxPayWithPoints(BigDecimal maxPayWithPoints) {
        this.maxPayWithPoints = maxPayWithPoints;
    }

    // Helper methods for budget checks
    public boolean isWithinTotalRedemptionsLimit() {
        return maxTotalRedemptions <= 0 || totalRedemptions < maxTotalRedemptions;
    }

    public boolean isWithinDailyRedemptionsLimit() {
        return maxRedemptionsPerDay <= 0 || redemptionsPerDay < maxRedemptionsPerDay;
    }

    public boolean isWithinMonthlyRedemptionsLimit() {
        return maxRedemptionsPerMonth <= 0 || redemptionsPerMonth < maxRedemptionsPerMonth;
    }

    public boolean isWithinPerCustomerLimit() {
        return maxPerCustomer <= 0 || perCustomerUsed < maxPerCustomer;
    }

    public boolean isWithinPerCustomerPerDayLimit() {
        return maxPerCustomerPerDay <= 0 || perCustomerPerDay < maxPerCustomerPerDay;
    }

    public boolean isWithinPerCustomerPerMonthLimit() {
        return maxPerCustomerPerMonth <= 0 || perCustomerPerMonth < maxPerCustomerPerMonth;
    }

    public boolean isWithinDiscountedAmountLimit() {
        return maxDiscountedAmount == null || totalDiscountedAmount.compareTo(maxDiscountedAmount) < 0;
    }

    public boolean isWithinOrdersValueLimit() {
        return maxOrdersValue == null || totalOrdersValue.compareTo(maxOrdersValue) < 0;
    }

    public boolean isWithinGiftAmountLimit() {
        return maxGiftAmount == null || totalGiftAmount.compareTo(maxGiftAmount) < 0;
    }

    public boolean isWithinPayWithPointsLimit() {
        return maxPayWithPoints == null || totalPayWithPoints.compareTo(maxPayWithPoints) < 0;
    }
}