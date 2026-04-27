package vn.viettel.vds.promotion.rule.engine.domain.model;

/**
 * Spec cho 1 quota check sau khi DRL fire xong.
 * DRL emit instance này qua {@code result.addPolicy(...)}; post-eval handler gọi
 * {@code QuotaCounterService.incrementWithCheck(bucketKey, limit)} cho mỗi policy.
 *
 * <p>Pha 1: chỉ cần {@code policyName + limit + bucketKey}. Pha 2 có thể thêm
 * {@code windowSeconds} cho rolling window, {@code incrementBy} cho nhiều redemption cùng lúc.
 */
public class QuotaPolicy {

    private final String policyName;  // e.g., "redemptions_per_customer_in_campaign_per_day"
    private final long limit;         // e.g., 3
    private final String bucketKey;   // e.g., "customer:cust-A:campaign:CMP-1:day:20260427"

    public QuotaPolicy(String policyName, long limit, String bucketKey) {
        this.policyName = policyName;
        this.limit = limit;
        this.bucketKey = bucketKey;
    }

    public String getPolicyName() {
        return policyName;
    }

    public long getLimit() {
        return limit;
    }

    public String getBucketKey() {
        return bucketKey;
    }

    @Override
    public String toString() {
        return String.format("QuotaPolicy{policyName='%s', limit=%d, bucketKey='%s'}",
                policyName, limit, bucketKey);
    }
}
