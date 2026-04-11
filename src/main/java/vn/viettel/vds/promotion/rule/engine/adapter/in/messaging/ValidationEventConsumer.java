package vn.viettel.vds.promotion.rule.engine.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.RuleConfigurationAdapter;
import vn.viettel.vds.promotion.rule.engine.application.service.AssignmentSyncService;
import vn.viettel.vds.promotion.validation.event.ValidationEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleDeletedEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleDisabledEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleEnabledEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleSettingAppliedEvent;
import vn.viettel.vds.promotion.validation.event.ValidationRuleSettingAppliedEventPayload;

import java.time.Instant;

/**
 * Kafka consumer for validation lifecycle events from pp-validation.
 * <p>
 * Deserializes the raw payload into a typed {@link ValidationEvent} via
 * Jackson polymorphic routing (on the {@code type} field) and then
 * dispatches to per-event handlers with {@code instanceof} pattern
 * matching. The payloads carry canonical {@code (subjectType, subjectKey)}
 * plus {@code bundleHash}, {@code sourceVersion}, and an embedded
 * {@code FastCheckConfigDto} snapshot so the rule engine can hydrate
 * both {@code assignments} and {@code fast_check_configs} without a
 * round-trip back to pp-validation.
 */
@Component
public class ValidationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ValidationEventConsumer.class);

    private final AssignmentSyncService assignmentSyncService;
    private final RuleConfigurationAdapter ruleConfigurationAdapter;
    private final ObjectMapper objectMapper;

    public ValidationEventConsumer(AssignmentSyncService assignmentSyncService,
                                   RuleConfigurationAdapter ruleConfigurationAdapter,
                                   ObjectMapper objectMapper) {
        this.assignmentSyncService = assignmentSyncService;
        this.ruleConfigurationAdapter = ruleConfigurationAdapter;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${kafka.topics.validation-events:promotion_validation_event}",
            groupId = "${promix.messaging.kafka.consumer.group-id:rule-engine-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onValidationEvent(String message, Acknowledgment ack) {
        try {
            ValidationEvent event = objectMapper.readValue(message, ValidationEvent.class);
            logger.info("Received validation event: type={}, id={}", event.getType(), event.getId());

            switch (event) {
                case ValidationRuleSettingAppliedEvent e -> handleApplied(e);
                case ValidationRuleEnabledEvent e -> handleEnabled(e);
                case ValidationRuleDisabledEvent e -> handleDisabled(e);
                case ValidationRuleDeletedEvent e -> handleDeleted(e);
                default -> logger.debug("Ignoring event type: {}", event.getType());
            }
        } catch (Exception e) {
            // Deserialization failures and handler errors are logged and acked:
            // the dead-letter path is handled at the container level via
            // ErrorHandlingDeserializer + DLQ topic suffix. We never want a
            // poison pill to block the partition.
            logger.error("Failed to process validation event: {}", message, e);
        } finally {
            ack.acknowledge();
        }
    }

    private void handleApplied(ValidationRuleSettingAppliedEvent event) {
        ValidationRuleSettingAppliedEventPayload payload = event.getPayload();
        if (payload == null) {
            logger.warn("Applied event {} has no payload", event.getId());
            return;
        }

        ValidationRuleSettingAppliedEventPayload.AssignmentResult assignment = payload.getAssignmentResult();
        ValidationRuleSettingAppliedEventPayload.ApplicabilityResult applicability = payload.getApplicabilityResult();
        ValidationRuleSettingAppliedEventPayload.TimeframeResult timeframe = payload.getTimeframeResult();

        if (assignment == null || applicability == null) {
            logger.warn("Applied event {} missing assignment or applicability result", event.getId());
            return;
        }

        String subjectType = applicability.getSubjectType();
        String subjectKey = applicability.getSubjectKey();
        String ruleId = assignment.getRuleId() != null ? assignment.getRuleId() : assignment.getAssignmentId();

        if (subjectType == null || subjectKey == null || ruleId == null) {
            logger.warn("Applied event {} missing subjectType/subjectKey/ruleId: {}:{}:{}",
                    event.getId(), subjectType, subjectKey, ruleId);
            return;
        }

        Instant validFrom = null;
        Instant validTo = null;
        String timezone = null;
        if (timeframe != null) {
            if (timeframe.getValidFrom() != null) {
                validFrom = Instant.ofEpochMilli(timeframe.getValidFrom());
            }
            if (timeframe.getValidTo() != null) {
                validTo = Instant.ofEpochMilli(timeframe.getValidTo());
            }
            timezone = timeframe.getTimezone();
        }

        AssignmentSyncService.SyncResult result = assignmentSyncService.upsertFromEvent(
                assignment.getAssignmentId(), ruleId, subjectType, subjectKey,
                Boolean.TRUE.equals(assignment.getActive()) || assignment.getActive() == null,
                assignment.getTrafficPercent(), assignment.getPriority(),
                assignment.getBundleHash(), validFrom, validTo, timezone);

        logger.info("Applied event processed: subject={}:{}, ruleId={}, bundleHash={}, needsCompile={}",
                subjectType, subjectKey, ruleId, assignment.getBundleHash(), result.needsCompile());

        if (payload.getFastCheckConfig() != null) {
            ruleConfigurationAdapter.upsert(subjectType, subjectKey,
                    payload.getFastCheckConfig(), assignment.getSourceVersion());
        }
    }

    private void handleEnabled(ValidationRuleEnabledEvent event) {
        var payload = event.getPayload();
        if (payload == null) {
            logger.warn("Enabled event {} missing payload", event.getId());
            return;
        }

        String subjectType = payload.getSubjectType();
        String subjectKey = payload.getSubjectKey();
        String ruleId = payload.getValidationRuleId();

        if (subjectType == null || subjectKey == null || ruleId == null) {
            logger.warn("Enabled event {} missing subject/rule fields: {}:{}:{}",
                    event.getId(), subjectType, subjectKey, ruleId);
            return;
        }

        assignmentSyncService.upsertFromEvent(
                null, ruleId, subjectType, subjectKey,
                true, null, null, payload.getBundleHash(),
                null, null, null);
        logger.info("Enabled event processed: subject={}:{}, ruleId={}", subjectType, subjectKey, ruleId);
    }

    private void handleDisabled(ValidationRuleDisabledEvent event) {
        var payload = event.getPayload();
        if (payload == null) {
            return;
        }
        String subjectType = payload.getSubjectType();
        String subjectKey = payload.getSubjectKey();
        String ruleId = payload.getValidationRuleId();

        if (subjectType == null || subjectKey == null || ruleId == null) {
            logger.warn("Disabled event {} missing subject/rule fields", event.getId());
            return;
        }

        assignmentSyncService.deactivateAssignment(subjectType, subjectKey, ruleId);
        logger.info("Disabled event processed: subject={}:{}, ruleId={}", subjectType, subjectKey, ruleId);
    }

    private void handleDeleted(ValidationRuleDeletedEvent event) {
        var payload = event.getPayload();
        if (payload == null) {
            return;
        }
        String subjectType = payload.getSubjectType();
        String subjectKey = payload.getSubjectKey();
        String ruleId = payload.getValidationRuleId();

        if (subjectType == null || subjectKey == null || ruleId == null) {
            logger.warn("Deleted event {} missing subject/rule fields", event.getId());
            return;
        }

        assignmentSyncService.deleteAssignment(subjectType, subjectKey, ruleId);
        ruleConfigurationAdapter.delete(subjectType, subjectKey);
        logger.info("Deleted event processed: subject={}:{}, ruleId={}", subjectType, subjectKey, ruleId);
    }
}
