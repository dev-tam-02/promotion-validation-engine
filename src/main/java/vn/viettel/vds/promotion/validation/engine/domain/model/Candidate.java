package vn.viettel.vds.promotion.validation.engine.domain.model;

public class Candidate {
    private String type;
    private String code;
    private String id;

    public Candidate() {
    }

    public Candidate(String type, String code) {
        this.type = type;
        this.code = code;
    }

    public Candidate(String type, String code, String id) {
        this.type = type;
        this.code = code;
        this.id = id;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isVoucher() {
        return "voucher".equals(type);
    }

    public boolean isTier() {
        return "tier".equals(type);
    }

    public boolean isLoyalty() {
        return "loyalty".equals(type);
    }
}