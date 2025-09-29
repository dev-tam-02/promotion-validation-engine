package vn.viettel.vds.promotion.validation.engine.domain.model;

import java.math.BigDecimal;
import java.util.*;

public class Order {
    private String id;
    private String currency;
    private BigDecimal total;
    private List<OrderItem> items = new ArrayList<>();
    private DiscountCtx discount;
    private Map<String, Object> metadata;
    private double totalAmount;
    private double initialAmount;
    private int itemsQuantity;
    private String channel;

    public Order() {
    }

    public Order(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(double initialAmount) {
        this.initialAmount = initialAmount;
    }

    public int getItemsQuantity() {
        return itemsQuantity;
    }

    public void setItemsQuantity(int itemsQuantity) {
        this.itemsQuantity = itemsQuantity;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public DiscountCtx getDiscount() {
        return discount;
    }

    public void setDiscount(DiscountCtx discount) {
        this.discount = discount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addItem(OrderItem item) {
        if (item != null) {
            this.items.add(item);
        }
    }

    // Helper methods used in DRL rules
    public boolean hasAnyItemInCategory(String category) {
        if (category == null) return false;
        return items.stream().anyMatch(i -> category.equals(i.getCategory()));
    }

    public boolean hasEveryItemInCategory(String category) {
        if (items.isEmpty()) return false;
        if (category == null) return false;
        return items.stream().allMatch(i -> category.equals(i.getCategory()));
    }

    public boolean hasNoItemInCategory(String category) {
        if (category == null) return true;
        return items.stream().noneMatch(i -> category.equals(i.getCategory()));
    }

    public OrderItem getMostExpensiveItem() {
        return items.stream().filter(Objects::nonNull)
                .max(Comparator.comparingDouble(OrderItem::getPrice))
                .orElse(null);
    }

    public OrderItem getCheapestItem() {
        return items.stream().filter(Objects::nonNull)
                .min(Comparator.comparingDouble(OrderItem::getPrice))
                .orElse(null);
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}