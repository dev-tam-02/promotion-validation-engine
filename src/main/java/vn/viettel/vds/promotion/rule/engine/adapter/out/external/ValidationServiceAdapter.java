package vn.viettel.vds.promotion.rule.engine.adapter.out.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ValidationServicePort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ValidationServiceAdapter implements ValidationServicePort {

    private static final Logger logger = LoggerFactory.getLogger(ValidationServiceAdapter.class);
    private static final String FIELD_TIME_WINDOWS = "timeWindows";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ValidationServiceAdapter(
            @Value("${validation.service.url:http://localhost:16014}") String baseUrl,
            @Value("${validation.service.context-path:/promotion/promotion-validation}") String contextPath,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl + contextPath)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RuleBindingDto> fetchActiveBindings(int page, int size) {
        try {
            String response = restClient.get()
                    .uri("/api/v1/rule-bindings/search?active=true&page={page}&size={size}&sortBy=createdAt&sortDirection=ASC",
                            page, size)
                    .retrieve()
                    .body(String.class);

            return parseBindingsFromSearchResponse(response);
        } catch (Exception e) {
            logger.error("Failed to fetch active bindings from pp-validation: page={}, size={}", page, size, e);
            return Collections.emptyList();
        }
    }

    @Override
    public RuleBindingDto fetchBindingByObjectAndRule(String objectType, String objectId, String ruleId) {
        try {
            String response = restClient.get()
                    .uri("/api/v1/rule-bindings/search?objectType={objectType}&objectId={objectId}&ruleId={ruleId}&size=1",
                            objectType, objectId, ruleId)
                    .retrieve()
                    .body(String.class);

            List<RuleBindingDto> bindings = parseBindingsFromSearchResponse(response);
            return bindings.isEmpty() ? null : bindings.getFirst();
        } catch (Exception e) {
            logger.error("Failed to fetch binding from pp-validation: objectType={}, objectId={}, ruleId={}",
                    objectType, objectId, ruleId, e);
            return null;
        }
    }

    private List<RuleBindingDto> parseBindingsFromSearchResponse(String response) {
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;
            JsonNode contentNode = dataNode.has("content") ? dataNode.get("content") : dataNode;

            if (!contentNode.isArray()) {
                return Collections.emptyList();
            }

            List<RuleBindingDto> result = new ArrayList<>();
            for (JsonNode node : contentNode) {
                result.add(parseBindingNode(node));
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to parse bindings response", e);
            return Collections.emptyList();
        }
    }

    private RuleBindingDto parseBindingNode(JsonNode node) {
        List<TimeWindowDto> timeWindows = new ArrayList<>();
        if (node.has(FIELD_TIME_WINDOWS) && node.get(FIELD_TIME_WINDOWS).isArray()) {
            for (JsonNode tw : node.get(FIELD_TIME_WINDOWS)) {
                timeWindows.add(new TimeWindowDto(
                        getTextOrNull(tw, "start"),
                        getTextOrNull(tw, "end")));
            }
        }

        return new RuleBindingDto(
                getTextOrNull(node, "id"),
                getTextOrNull(node, "ruleId"),
                getTextOrNull(node, "objectType"),
                getTextOrNull(node, "objectId"),
                getIntOrNull(node, "priority"),
                getBooleanOrNull(node, "active"),
                getInstantOrNull(node, "validFrom"),
                getInstantOrNull(node, "validTo"),
                getTextOrNull(node, "timezone"),
                getTextOrNull(node, "rrule"),
                timeWindows,
                getStringList(node, "excludedDates"),
                getBooleanOrNull(node, "includedAll"),
                getStringList(node, "includedProducts"),
                getStringList(node, "excludedProducts"),
                getStringList(node, "includedCategories"),
                getStringList(node, "excludedCategories"),
                getStringList(node, "includedBrands"),
                getStringList(node, "excludedBrands"),
                getIntOrNull(node, "trafficPercent"),
                getTextOrNull(node, "stickyKeyStrategy"),
                getTextOrNull(node, "bundleHash"),
                getLongOrNull(node, "version")
        );
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Integer getIntOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : null;
    }

    private Long getLongOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asLong() : null;
    }

    private Boolean getBooleanOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asBoolean() : null;
    }

    private Instant getInstantOrNull(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        try {
            return Instant.parse(node.get(field).asText());
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> getStringList(JsonNode node, String field) {
        if (!node.has(field) || !node.get(field).isArray()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node.get(field)) {
            result.add(item.asText());
        }
        return result;
    }
}
