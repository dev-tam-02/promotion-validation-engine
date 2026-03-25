package vn.viettel.vds.promotion.rule.engine.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.application.service.AssignmentSyncService;

import java.time.Instant;

/**
 * Kafka consumer for validation events from pp-validation.
 * Handles assignment sync when rules are applied, deleted, enabled, or disabled.
 */
@Component
public class ValidationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEventConsumer.class);

    private final AssignmentSyncService assignmentSyncService;
    private final ObjectMapper objectMapper;

    public ValidationEventConsumer(AssignmentSyncService assignmentSyncService,
                                   ObjectMapper objectMapper) {
        this.assignmentSyncService = assignmentSyncService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${kafka.topics.validation-events:promotion_validation_event}",
            groupId = "${promix.messaging.kafka.consumer.group-id:rule-engine-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onValidationEvent(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = getTextOrNull(root, "type");

            if (eventType == null) {
                logger.warn("Received validation event without type, skipping");
                ack.acknowledge();
                return;
            }

            logger.info("Received validation event: type={}", eventType);

            switch (eventType) {
                case "ValidationRuleSettingAppliedEvent" -> handleAppliedEvent(root);
                case "ValidationRuleDeletedEvent" -> handleDeletedEvent(root);
                case "ValidationRuleDisabledEvent" -> handleDisabledEvent(root);
                case "ValidationRuleEnabledEvent" -> handleEnabledEvent(root);
                case "ValidationCompensationResultEvent" -> handleCompensationEvent(root);
                default -> logger.debug("Ignoring validation event type: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            logger.error("Failed to process validation event", e);
            ack.acknowledge();
        }
    }

    private void handleAppliedEvent(JsonNode root) {
        JsonNode payload = root.get("payload");
        if (payload == null) {
            logger.warn("Applied event missing payload");
            return;
        }

        String subject = getTextOrNull(root, "subject");

        JsonNode assignmentResult = payload.get("assignmentResult");
        JsonNode timeframeResult = payload.get("timeframeResult");
        JsonNode applicabilityResult = payload.get("applicabilityResult");

        if (assignmentResult == null) {
            logger.warn("Applied event missing assignmentResult");
            return;
        }

        String assignmentId = getTextOrNull(assignmentResult, "assignmentId");
        String ruleId = getTextOrNull(assignmentResult, "ruleId");
        boolean active = assignmentResult.has("active") && assignmentResult.get("active").asBoolean(true);
        Integer trafficPercent = getIntOrNull(assignmentResult, "trafficPercent");
        Integer priority = getIntOrNull(assignmentResult, "priority");

        // pp-validation uses assignmentId (binding ID) as the ruleId for compilation
        // When ruleId is null in the event, fall back to assignmentId
        if (ruleId == null) {
            ruleId = assignmentId;
        }

        // Use DISCOUNT_COUPON as default subjectType for campaign-originated events
        // applicabilityResult.subjectType is "PRODUCT" (scope), not the campaign subject type
        String subjectType = "DISCOUNT_COUPON";
        String subjectKey = subject;

        if (subjectKey == null) {
            subjectKey = ruleId;
        }

        // Extract timeframe
        Instant validFrom = null;
        Instant validTo = null;
        String timezone = null;
        if (timeframeResult != null) {
            Long validFromMs = getLongOrNull(timeframeResult, "validFrom");
            Long validToMs = getLongOrNull(timeframeResult, "validTo");
            if (validFromMs != null) {
                validFrom = Instant.ofEpochMilli(validFromMs);
            }
            if (validToMs != null) {
                validTo = Instant.ofEpochMilli(validToMs);
            }
            timezone = getTextOrNull(timeframeResult, "timezone");
        }

        AssignmentSyncService.SyncResult result = assignmentSyncService.upsertFromEvent(
                assignmentId, ruleId, subjectType, subjectKey,
                active, trafficPercent, priority,
                null, validFrom, validTo, timezone);

        logger.info("Processed applied event: assignmentId={}, ruleId={}, subjectType={}, subjectKey={}, needsCompile={}",
                assignmentId, ruleId, subjectType, subjectKey, result.needsCompile());
    }

    private void handleDeletedEvent(JsonNode root) {
        String ruleId = extractRuleId(root);
        String subject = getTextOrNull(root, "subject");

        if (ruleId != null && subject != null) {
            assignmentSyncService.deleteAssignment("CAMPAIGN", subject, ruleId);
            logger.info("Processed deleted event: ruleId={}, subject={}", ruleId, subject);
        }
    }

    private void handleDisabledEvent(JsonNode root) {
        String ruleId = extractRuleId(root);
        String subject = getTextOrNull(root, "subject");

        if (ruleId != null && subject != null) {
            assignmentSyncService.deactivateAssignment("CAMPAIGN", subject, ruleId);
            logger.info("Processed disabled event: ruleId={}, subject={}", ruleId, subject);
        }
    }

    private void handleEnabledEvent(JsonNode root) {
        String ruleId = extractRuleId(root);
        String subject = getTextOrNull(root, "subject");

        if (ruleId != null && subject != null) {
            AssignmentSyncService.SyncResult result = assignmentSyncService.upsertFromEvent(
                    null, ruleId, "CAMPAIGN", subject,
                    true, null, null,
                    null, null, null, null);
            logger.info("Processed enabled event: ruleId={}, subject={}", ruleId, subject);
        }
    }

    private void handleCompensationEvent(JsonNode root) {
        JsonNode payload = root.get("payload");
        if (payload == null) {
            return;
        }

        JsonNode compensationResult = payload.get("compensationResult");
        if (compensationResult == null) {
            return;
        }

        String rollbackAction = getTextOrNull(compensationResult, "rollbackAction");
        String ruleId = getTextOrNull(compensationResult, "ruleId");
        String assignmentId = getTextOrNull(compensationResult, "assignmentId");
        String subject = getTextOrNull(root, "subject");

        if (rollbackAction == null || ruleId == null) {
            return;
        }

        switch (rollbackAction) {
            case "DELETE" -> {
                if (subject != null) {
                    assignmentSyncService.deleteAssignment("CAMPAIGN", subject, ruleId);
                }
            }
            case "DEACTIVATE", "DISABLE" -> {
                if (subject != null) {
                    assignmentSyncService.deactivateAssignment("CAMPAIGN", subject, ruleId);
                }
            }
            case "ENABLE" -> {
                if (subject != null) {
                    assignmentSyncService.upsertFromEvent(
                            assignmentId, ruleId, "CAMPAIGN", subject,
                            true, null, null,
                            null, null, null, null);
                }
            }
            default -> logger.debug("Ignoring compensation rollbackAction: {}", rollbackAction);
        }

        logger.info("Processed compensation event: rollbackAction={}, ruleId={}, subject={}", rollbackAction, ruleId, subject);
    }

    private String extractRuleId(JsonNode root) {
        // Try from payload first
        JsonNode payload = root.get("payload");
        if (payload != null) {
            JsonNode compensationResult = payload.get("compensationResult");
            if (compensationResult != null) {
                String ruleId = getTextOrNull(compensationResult, "ruleId");
                if (ruleId != null) return ruleId;
            }
        }
        // Try from root level
        return getTextOrNull(root, "ruleId");
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
}
