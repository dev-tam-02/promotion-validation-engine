package vn.viettel.vds.promotion.validation.engine.domain.model;

public class Redemption {
    private boolean codeHolder;
    private int perCustomerPerDay;
    private int perCustomerTotal;
    private boolean redeemingCodeHolder;
    private String locationId;

    public Redemption() {
    }

    public Redemption(boolean redeemingCodeHolder, String locationId) {
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
}