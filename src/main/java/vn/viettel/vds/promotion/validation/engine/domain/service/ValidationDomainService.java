package vn.viettel.vds.promotion.validation.engine.domain.service;

import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.domain.model.Customer;
import vn.viettel.vds.promotion.validation.engine.domain.model.Order;
import vn.viettel.vds.promotion.validation.engine.domain.model.Redemption;
import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;

@Service
public class ValidationDomainService {

    public ValidationResult validatePromotion(Customer customer, Order order) {
        return validatePromotion(customer, order, null);
    }

    public ValidationResult validatePromotion(Customer customer, Order order, Redemption redemption) {
        // Domain-level validation logic
        if (customer == null || order == null) {
            return new ValidationResult(false, "Customer and order are required");
        }

        // Basic domain rules can be applied here
        if (order.getTotalAmount() <= 0) {
            return new ValidationResult(false, "Order total amount must be positive");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            return new ValidationResult(false, "Order must contain at least one item");
        }

        // This method will be used in coordination with rules engine
        // The actual business rules validation will be delegated to the rules engine
        return new ValidationResult(true, "Domain validation passed");
    }

    public boolean isCustomerEligible(Customer customer) {
        if (customer == null) {
            return false;
        }

        // Basic eligibility checks
        return customer.getId() != null && !customer.getId().trim().isEmpty();
    }

    public boolean isOrderValid(Order order) {
        if (order == null) {
            return false;
        }

        return order.getId() != null &&
                !order.getId().trim().isEmpty() &&
                order.getTotalAmount() > 0 &&
                order.getItems() != null &&
                !order.getItems().isEmpty();
    }
}