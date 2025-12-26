package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.math.BigDecimal;

public class DiscountCtx {
    private BigDecimal amount;
    private String type;
    private String code;

    public DiscountCtx() {
    }

    public DiscountCtx(BigDecimal amount, String type, String code) {
        this.amount = amount;
        this.type = type;
        this.code = code;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}