package vn.viettel.vds.promotion.rule.engine.application.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for composing Redis bucket key strings used by {@link QuotaCounterService}.
 *
 * <p>Bucket keys encode the scope dimension (incentive, customer-campaign) and the time window
 * (per-day, per-month, or sticky/no-window). The composed key is passed to
 * {@link QuotaCounterService#incrementWithCheck} as {@code bucketKey}.
 *
 * <p>Timezone: {@code Asia/Ho_Chi_Minh} is hard-coded for Phase 1 to match VN business logic.
 * Phase 2: read from tenant config if multi-timezone support is needed.
 */
public final class BucketKeys {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(VN_ZONE);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM").withZone(VN_ZONE);

    private BucketKeys() {
        // utility class — no instantiation
    }

    /**
     * Scope: redemptions per incentive per day.
     * Pattern: {@code incentive:{incentiveId}:day:{yyyyMMdd}}
     */
    public static String incentivePerDay(String incentiveId, Instant now) {
        return String.format("incentive:%s:day:%s", incentiveId, DAY_FMT.format(now));
    }

    /**
     * Scope: redemptions per incentive per month.
     * Pattern: {@code incentive:{incentiveId}:month:{yyyyMM}}
     */
    public static String incentivePerMonth(String incentiveId, Instant now) {
        return String.format("incentive:%s:month:%s", incentiveId, MONTH_FMT.format(now));
    }

    /**
     * Scope: total redemptions per customer in campaign (sticky, no time window).
     * Pattern: {@code customer:{customerId}:campaign:{campaignId}}
     */
    public static String customerInCampaign(String customerId, String campaignId) {
        return String.format("customer:%s:campaign:%s", customerId, campaignId);
    }

    /**
     * Scope: redemptions per customer in campaign per day.
     * Pattern: {@code customer:{customerId}:campaign:{campaignId}:day:{yyyyMMdd}}
     */
    public static String customerInCampaignPerDay(String customerId, String campaignId, Instant now) {
        return String.format("customer:%s:campaign:%s:day:%s", customerId, campaignId, DAY_FMT.format(now));
    }

    /**
     * Scope: redemptions per customer in campaign per month.
     * Pattern: {@code customer:{customerId}:campaign:{campaignId}:month:{yyyyMM}}
     */
    public static String customerInCampaignPerMonth(String customerId, String campaignId, Instant now) {
        return String.format("customer:%s:campaign:%s:month:%s", customerId, campaignId, MONTH_FMT.format(now));
    }
}
