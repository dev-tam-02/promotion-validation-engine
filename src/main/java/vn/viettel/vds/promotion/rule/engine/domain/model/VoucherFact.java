package vn.viettel.vds.promotion.rule.engine.domain.model;

/**
 * Drools fact representing a voucher/coupon code instance.
 *
 * <p>Used by rules that need to inspect voucher-level attributes such as the
 * owner constraint (owner-only rule). Populated by {@code FactPreparationService}
 * from the {@code "voucher"} key in the execution context.
 *
 * <p>Fields mirror the redemption fact payload defined in §4.4 of the design spec:
 * <pre>
 * {
 *   "voucher": {
 *     "code": "SALE-A1B2",
 *     "ownerCustomerId": "cust-123",
 *     "maxUsesPerCode": 3,
 *     "usedCount": 1
 *   }
 * }
 * </pre>
 */
public class VoucherFact {

    private String code;
    private String ownerCustomerId;
    private Integer maxUsesPerCode;
    private Integer usedCount;

    public VoucherFact() {
    }

    public VoucherFact(String code, String ownerCustomerId) {
        this.code = code;
        this.ownerCustomerId = ownerCustomerId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOwnerCustomerId() {
        return ownerCustomerId;
    }

    public void setOwnerCustomerId(String ownerCustomerId) {
        this.ownerCustomerId = ownerCustomerId;
    }

    public Integer getMaxUsesPerCode() {
        return maxUsesPerCode;
    }

    public void setMaxUsesPerCode(Integer maxUsesPerCode) {
        this.maxUsesPerCode = maxUsesPerCode;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }
}
