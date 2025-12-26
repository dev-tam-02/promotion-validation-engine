package vn.viettel.vds.promotion.rule.engine.domain.model;

public class OrderItem {
    private String productId;
    private String productName;
    private String category;
    private String sku;
    private String ruleCode;
    private double price;
    private int quantity;

    public OrderItem() {
    }

    public OrderItem(String productName, String category, String sku, double price, int quantity) {
        this.productName = productName;
        this.category = category;
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}