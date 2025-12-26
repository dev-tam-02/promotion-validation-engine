package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.CandidateDto;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.CustomerDto;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.OrderDto;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.OrderItemDto;
import vn.viettel.vds.promotion.rule.engine.domain.model.Candidate;
import vn.viettel.vds.promotion.rule.engine.domain.model.Customer;
import vn.viettel.vds.promotion.rule.engine.domain.model.Order;
import vn.viettel.vds.promotion.rule.engine.domain.model.OrderItem;

import java.math.BigDecimal;
import java.util.*;

@Service
public class FactPreparationService {

    private static final Logger logger = LoggerFactory.getLogger(FactPreparationService.class);

    // Constants for commonly used attribute keys
    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_PHONE = "phone";

    public List<Object> prepareFacts(Map<String, Object> context) {
        logger.debug("Preparing facts from context with {} entries", context.size());

        List<Object> facts = new ArrayList<>();

        // Add domain object facts
        addCustomerFacts(context.get("customer"), facts);
        addOrderFacts(context.get("order"), facts);
        addCandidateFacts(context.get("candidate"), facts);
        addExecutionContextFacts(context.get("executionContext"), facts);

        logger.debug("Prepared {} total facts for rule execution", facts.size());
        return facts;
    }

    private void addCustomerFacts(Object customerObj, List<Object> facts) {
        if (customerObj == null) {
            return;
        }

        Customer customer = convertToCustomer(customerObj);
        if (customer != null) {
            facts.add(customer);
            logger.debug("Added Customer fact: id={}", customer.getId());
        }
    }

    private void addOrderFacts(Object orderObj, List<Object> facts) {
        if (orderObj == null) {
            return;
        }

        Order order = convertToOrder(orderObj);
        if (order != null) {
            facts.add(order);
            logger.debug("Added Order fact: id={}, total={}", order.getId(), order.getTotal());
            addOrderItemFacts(order, facts);
        }
    }

    private void addOrderItemFacts(Order order, List<Object> facts) {
        if (order.getItems() == null) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            facts.add(item);
        }
        logger.debug("Added {} OrderItem facts", order.getItems().size());
    }

    private void addCandidateFacts(Object candidateObj, List<Object> facts) {
        if (candidateObj == null) {
            return;
        }

        Candidate candidate = convertToCandidate(candidateObj);
        if (candidate != null) {
            facts.add(candidate);
            logger.debug("Added Candidate fact: id={}, type={}", candidate.getId(), candidate.getType());
        }
    }

    private void addExecutionContextFacts(Object executionContextObj, List<Object> facts) {
        if (executionContextObj instanceof Map<?, ?> execContext) {
            facts.addAll(prepareContextFacts((Map<String, Object>) execContext));
        }
    }

    private Customer convertToCustomer(Object customerObj) {
        try {
            if (customerObj instanceof Customer customer) {
                return customer;
            }

            if (customerObj instanceof CustomerDto dto) {
                return convertFromCustomerDto(dto);
            }

            if (customerObj instanceof Map<?, ?> customerMap) {
                return convertFromCustomerMap((Map<String, Object>) customerMap);
            }

            logger.warn("Unable to convert customer object of type: {}", customerObj.getClass());
            return null;
        } catch (Exception e) {
            logger.error("Error converting customer object", e);
            return null;
        }
    }

    private Customer convertFromCustomerDto(CustomerDto dto) {
        Customer customer = new Customer();
        customer.setId(dto.id());

        // Convert List to Set for segments
        if (dto.segments() != null) {
            customer.setSegments(new java.util.HashSet<>(dto.segments()));
        }

        // Map tier Integer to loyaltyTier String
        if (dto.tier() != null) {
            customer.setLoyaltyTier(String.valueOf(dto.tier()));
        }

        // Build attributes from metadata and region
        Map<String, Object> attrs = new HashMap<>();
        if (dto.metadata() != null) {
            attrs.putAll(dto.metadata());
        }
        if (dto.region() != null) {
            attrs.put("region", dto.region());
        }
        customer.setAttrs(attrs);

        logger.info("CONVERTED CUSTOMER - id={}, segments={}, tier={}, region={}",
                customer.getId(), customer.getSegments(), customer.getLoyaltyTier(), dto.region());

        return customer;
    }

    private Customer convertFromCustomerMap(Map<String, Object> customerMap) {
        Customer customer = new Customer();
        customer.setId((String) customerMap.get("id"));
        customer.setLoyaltyTier((String) customerMap.get("tier"));
        customer.setAttrs(buildCustomerAttributes(customerMap));
        return customer;
    }

    private Map<String, Object> buildCustomerAttributes(Map<String, Object> customerMap) {
        Map<String, Object> attrs = new HashMap<>();
        addAttributeIfPresent(attrs, customerMap, "name");
        addAttributeIfPresent(attrs, customerMap, ATTR_EMAIL);
        addAttributeIfPresent(attrs, customerMap, ATTR_PHONE);
        return attrs;
    }

    private void addAttributeIfPresent(Map<String, Object> attrs, Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value != null) {
            attrs.put(key, value);
        }
    }

    private Order convertToOrder(Object orderObj) {
        try {
            if (orderObj instanceof Order order) {
                return order;
            }

            if (orderObj instanceof OrderDto dto) {
                return convertFromOrderDto(dto);
            }

            if (orderObj instanceof Map<?, ?> orderMap) {
                return convertFromOrderMap((Map<String, Object>) orderMap);
            }

            logger.warn("Unable to convert order object of type: {}", orderObj.getClass());
            return null;
        } catch (Exception e) {
            logger.error("Error converting order object", e);
            return null;
        }
    }

    private Order convertFromOrderDto(OrderDto dto) {
        Order order = new Order();
        order.setId(dto.id());
        order.setCurrency(dto.currency());
        order.setTotal(dto.total());
        order.setItems(convertOrderItemsFromDto(dto.items()));
        logger.info("CONVERTED ORDER - id={}, total={}, currency={}, items={}",
                order.getId(), order.getTotal(), order.getCurrency(),
                order.getItems() != null ? order.getItems().size() : 0);
        return order;
    }

    private List<OrderItem> convertOrderItemsFromDto(List<OrderItemDto> itemDtos) {
        if (itemDtos == null) {
            return Collections.emptyList();
        }

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemDto itemDto : itemDtos) {
            OrderItem item = new OrderItem();
            item.setProductId(itemDto.productId());  // Map productId to productId field
            item.setSku(itemDto.skuId());            // Map skuId to sku field
            item.setQuantity(itemDto.quantity());
            item.setPrice(itemDto.price().doubleValue());
            item.setCategory(itemDto.category());
            items.add(item);
        }
        return items;
    }

    private Order convertFromOrderMap(Map<String, Object> orderMap) {
        Order order = new Order();
        order.setId((String) orderMap.get("id"));
        order.setCurrency((String) orderMap.get("currency"));
        setOrderTotal(order, orderMap.get("total"));
        setOrderItems(order, orderMap.get("items"));
        return order;
    }

    private void setOrderTotal(Order order, Object totalObj) {
        if (totalObj instanceof Number number) {
            order.setTotal(BigDecimal.valueOf(number.doubleValue()));
        }
    }

    private void setOrderItems(Order order, Object itemsObj) {
        if (!(itemsObj instanceof List<?> itemsList)) {
            return;
        }

        List<OrderItem> items = new ArrayList<>();
        for (Object itemObj : itemsList) {
            OrderItem item = convertToOrderItem(itemObj);
            if (item != null) {
                items.add(item);
            }
        }
        order.setItems(items);
    }

    private OrderItem convertToOrderItem(Object itemObj) {
        try {
            if (itemObj instanceof OrderItem orderItem) {
                return orderItem;
            }

            if (itemObj instanceof Map) {
                Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                OrderItem item = new OrderItem();

                item.setProductId((String) itemMap.get("productId")); // Map productId to productId field
                item.setSku((String) itemMap.get("skuId"));           // Map skuId to sku field
                item.setCategory((String) itemMap.get("category"));
                item.setProductName((String) itemMap.get("name"));    // Set product name if available

                Object priceObj = itemMap.get("price");
                if (priceObj instanceof Number number) {
                    item.setPrice(number.doubleValue());
                }

                Object quantityObj = itemMap.get("quantity");
                if (quantityObj instanceof Number number) {
                    item.setQuantity(number.intValue());
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
            if (candidateObj instanceof Candidate candidate) {
                return candidate;
            }

            // Handle CandidateDto record
            if (candidateObj instanceof CandidateDto dto) {
                Candidate candidate = new Candidate();
                candidate.setId(dto.id());
                candidate.setType(dto.type());
                // CandidateDto doesn't have code field, leave it null
                return candidate;
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
        if (timestampObj instanceof Number number) {
            contextFacts.add(new ExecutionTimestamp(number.longValue()));
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