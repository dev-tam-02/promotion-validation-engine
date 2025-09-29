package vn.viettel.vds.promotion.validation.engine.domain.service.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;
import vn.viettel.vds.promotion.validation.engine.domain.model.Customer;
import vn.viettel.vds.promotion.validation.engine.domain.model.Order;
import vn.viettel.vds.promotion.validation.engine.domain.model.OrderItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FactPreparationService {

    private static final Logger logger = LoggerFactory.getLogger(FactPreparationService.class);

    public List<Object> prepareFacts(Map<String, Object> context) {
        logger.debug("Preparing facts from context with {} entries", context.size());

        List<Object> facts = new ArrayList<>();

        // Extract and validate core domain objects
        Object customerObj = context.get("customer");
        Object orderObj = context.get("order");
        Object candidateObj = context.get("candidate");
        Object executionContextObj = context.get("executionContext");

        // Add customer if present
        if (customerObj != null) {
            Customer customer = convertToCustomer(customerObj);
            if (customer != null) {
                facts.add(customer);
                logger.debug("Added Customer fact: id={}", customer.getId());
            }
        }

        // Add order if present
        if (orderObj != null) {
            Order order = convertToOrder(orderObj);
            if (order != null) {
                facts.add(order);
                logger.debug("Added Order fact: id={}, total={}", order.getId(), order.getTotal());

                // Add order items as individual facts
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        facts.add(item);
                    }
                    logger.debug("Added {} OrderItem facts", order.getItems().size());
                }
            }
        }

        // Add candidate if present
        if (candidateObj != null) {
            Candidate candidate = convertToCandidate(candidateObj);
            if (candidate != null) {
                facts.add(candidate);
                logger.debug("Added Candidate fact: id={}, type={}", candidate.getId(), candidate.getType());
            }
        }

        // Add execution context metadata as facts
        if (executionContextObj instanceof Map) {
            Map<String, Object> execContext = (Map<String, Object>) executionContextObj;
            facts.addAll(prepareContextFacts(execContext));
        }

        logger.debug("Prepared {} total facts for rule execution", facts.size());
        return facts;
    }

    private Customer convertToCustomer(Object customerObj) {
        try {
            if (customerObj instanceof Customer) {
                return (Customer) customerObj;
            }

            if (customerObj instanceof Map) {
                Map<String, Object> customerMap = (Map<String, Object>) customerObj;
                Customer customer = new Customer();

                customer.setId((String) customerMap.get("id"));
                customer.setLoyaltyTier((String) customerMap.get("tier"));

                // Set attributes from map
                Map<String, Object> attrs = new HashMap<>();
                if (customerMap.get("name") != null) {
                    attrs.put("name", customerMap.get("name"));
                }
                if (customerMap.get("email") != null) {
                    attrs.put("email", customerMap.get("email"));
                }
                if (customerMap.get("phone") != null) {
                    attrs.put("phone", customerMap.get("phone"));
                }
                customer.setAttrs(attrs);

                return customer;
            }

            logger.warn("Unable to convert customer object of type: {}", customerObj.getClass());
            return null;
        } catch (Exception e) {
            logger.error("Error converting customer object", e);
            return null;
        }
    }

    private Order convertToOrder(Object orderObj) {
        try {
            if (orderObj instanceof Order) {
                return (Order) orderObj;
            }

            if (orderObj instanceof Map) {
                Map<String, Object> orderMap = (Map<String, Object>) orderObj;
                Order order = new Order();

                order.setId((String) orderMap.get("id"));
                order.setCurrency((String) orderMap.get("currency"));

                // Convert total from various number types
                Object totalObj = orderMap.get("total");
                if (totalObj instanceof Number) {
                    order.setTotal(BigDecimal.valueOf(((Number) totalObj).doubleValue()));
                }

                // Convert items
                Object itemsObj = orderMap.get("items");
                if (itemsObj instanceof List) {
                    List<OrderItem> items = new ArrayList<>();
                    List<?> itemsList = (List<?>) itemsObj;

                    for (Object itemObj : itemsList) {
                        OrderItem item = convertToOrderItem(itemObj);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                    order.setItems(items);
                }

                return order;
            }

            logger.warn("Unable to convert order object of type: {}", orderObj.getClass());
            return null;
        } catch (Exception e) {
            logger.error("Error converting order object", e);
            return null;
        }
    }

    private OrderItem convertToOrderItem(Object itemObj) {
        try {
            if (itemObj instanceof OrderItem) {
                return (OrderItem) itemObj;
            }

            if (itemObj instanceof Map) {
                Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                OrderItem item = new OrderItem();

                item.setSku((String) itemMap.get("id")); // Map id to sku
                item.setCategory((String) itemMap.get("category"));
                item.setSku((String) itemMap.get("productId")); // Map productId to sku
                item.setProductName((String) itemMap.get("name"));

                Object priceObj = itemMap.get("price");
                if (priceObj instanceof Number) {
                    item.setPrice(((Number) priceObj).doubleValue());
                }

                Object quantityObj = itemMap.get("quantity");
                if (quantityObj instanceof Number) {
                    item.setQuantity(((Number) quantityObj).intValue());
                }

                return item;
            }

            return null;
        } catch (Exception e) {
            logger.error("Error converting order item object", e);
            return null;
        }
    }

    private Candidate convertToCandidate(Object candidateObj) {
        try {
            if (candidateObj instanceof Candidate) {
                return (Candidate) candidateObj;
            }

            if (candidateObj instanceof Map) {
                Map<String, Object> candidateMap = (Map<String, Object>) candidateObj;
                Candidate candidate = new Candidate();

                candidate.setId((String) candidateMap.get("id"));
                candidate.setType((String) candidateMap.get("type"));
                candidate.setCode((String) candidateMap.get("code"));

                // Note: Candidate model doesn't have name/description,
                // could be added to the model or stored in metadata if needed

                return candidate;
            }

            logger.warn("Unable to convert candidate object of type: {}", candidateObj.getClass());
            return null;
        } catch (Exception e) {
            logger.error("Error converting candidate object", e);
            return null;
        }
    }

    private List<Object> prepareContextFacts(Map<String, Object> executionContext) {
        List<Object> contextFacts = new ArrayList<>();

        // Add tenant context
        String tenantId = (String) executionContext.get("tenantId");
        if (tenantId != null) {
            contextFacts.add(new TenantContext(tenantId));
        }

        // Add timestamp context
        Object timestampObj = executionContext.get("timestamp");
        if (timestampObj instanceof Number) {
            contextFacts.add(new ExecutionTimestamp(((Number) timestampObj).longValue()));
        }

        // Add any custom metadata as generic facts
        Object metadataObj = executionContext.get("metadata");
        if (metadataObj instanceof Map) {
            Map<String, Object> metadata = (Map<String, Object>) metadataObj;
            contextFacts.add(new ExecutionMetadata(metadata));
        }

        return contextFacts;
    }

    // Helper classes for context facts
    public static class TenantContext {
        private final String tenantId;

        public TenantContext(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getTenantId() {
            return tenantId;
        }
    }

    public static class ExecutionTimestamp {
        private final long timestamp;

        public ExecutionTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class ExecutionMetadata {
        private final Map<String, Object> metadata;

        public ExecutionMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public Object get(String key) {
            return metadata.get(key);
        }
    }
}