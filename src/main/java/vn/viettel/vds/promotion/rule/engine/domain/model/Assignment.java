package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.time.Instant;
import java.util.List;

public class Assignment {

    private String id;
    private String subjectType;
    private String subjectKey;
    private String ruleId;
    private String bundleHash;
    private boolean active;
    private int priority;
    private Instant validFrom;
    private Instant validTo;
    private String timezone;
    private String rrule;
    private List<String> timeWindows;
    private List<String> excludedDates;
    private int trafficPercent;
    private String stickyKeyStrategy;
    private boolean includedAll;
    private List<String> includedProducts;
    private List<String> excludedProducts;
    private List<String> includedCategories;
    private List<String> excludedCategories;
    private List<String> includedBrands;
    private List<String> excludedBrands;
    private long sourceVersion;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public void setSubjectKey(String subjectKey) {
        this.subjectKey = subjectKey;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getBundleHash() {
        return bundleHash;
    }

    public void setBundleHash(String bundleHash) {
        this.bundleHash = bundleHash;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getRrule() {
        return rrule;
    }

    public void setRrule(String rrule) {
        this.rrule = rrule;
    }

    public List<String> getTimeWindows() {
        return timeWindows;
    }

    public void setTimeWindows(List<String> timeWindows) {
        this.timeWindows = timeWindows;
    }

    public List<String> getExcludedDates() {
        return excludedDates;
    }

    public void setExcludedDates(List<String> excludedDates) {
        this.excludedDates = excludedDates;
    }

    public int getTrafficPercent() {
        return trafficPercent;
    }

    public void setTrafficPercent(int trafficPercent) {
        this.trafficPercent = trafficPercent;
    }

    public String getStickyKeyStrategy() {
        return stickyKeyStrategy;
    }

    public void setStickyKeyStrategy(String stickyKeyStrategy) {
        this.stickyKeyStrategy = stickyKeyStrategy;
    }

    public boolean isIncludedAll() {
        return includedAll;
    }

    public void setIncludedAll(boolean includedAll) {
        this.includedAll = includedAll;
    }

    public List<String> getIncludedProducts() {
        return includedProducts;
    }

    public void setIncludedProducts(List<String> includedProducts) {
        this.includedProducts = includedProducts;
    }

    public List<String> getExcludedProducts() {
        return excludedProducts;
    }

    public void setExcludedProducts(List<String> excludedProducts) {
        this.excludedProducts = excludedProducts;
    }

    public List<String> getIncludedCategories() {
        return includedCategories;
    }

    public void setIncludedCategories(List<String> includedCategories) {
        this.includedCategories = includedCategories;
    }

    public List<String> getExcludedCategories() {
        return excludedCategories;
    }

    public void setExcludedCategories(List<String> excludedCategories) {
        this.excludedCategories = excludedCategories;
    }

    public List<String> getIncludedBrands() {
        return includedBrands;
    }

    public void setIncludedBrands(List<String> includedBrands) {
        this.includedBrands = includedBrands;
    }

    public List<String> getExcludedBrands() {
        return excludedBrands;
    }

    public void setExcludedBrands(List<String> excludedBrands) {
        this.excludedBrands = excludedBrands;
    }

    public long getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(long sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
