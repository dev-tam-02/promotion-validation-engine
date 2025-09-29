package vn.viettel.vds.promotion.validation.engine.domain.model;

import java.time.LocalDate;

public class LimitsCtx {
    private int perCodeTotalUsed;
    private int perCustomerUsed;
    private LocalDate date;

    public LimitsCtx() {
    }

    public LimitsCtx(int perCodeTotalUsed, int perCustomerUsed, LocalDate date) {
        this.perCodeTotalUsed = perCodeTotalUsed;
        this.perCustomerUsed = perCustomerUsed;
        this.date = date;
    }

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
}