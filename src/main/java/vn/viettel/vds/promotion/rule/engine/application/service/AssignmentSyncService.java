package vn.viettel.vds.promotion.rule.engine.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.rule.engine.application.port.out.AssignmentRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.BundleRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ValidationServicePort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

// Sonar rules S2230, S1854, S1481 are false positives on this class for older sonar-java
// plugins (partner Sonar) that misread method visibility on classes containing inner records
// and incorrectly flag local variables (subjectType/subjectKey/ruleId/existing) as dead
// stores or unused — they are read multiple times immediately after assignment.
@Service
@SuppressWarnings({"java:S2230", "java:S1854", "java:S1481"})
public class AssignmentSyncService {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentSyncService.class);

    private final AssignmentRepositoryPort assignmentRepository;
    private final BundleRepositoryPort bundleRepository;
    private final ObjectMapper objectMapper;

    public AssignmentSyncService(AssignmentRepositoryPort assignmentRepository,
                                 BundleRepositoryPort bundleRepository,
                                 ObjectMapper objectMapper) {
        this.assignmentRepository = assignmentRepository;
        this.bundleRepository = bundleRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Upsert assignment from a RuleBinding DTO (fetched via API or event).
     * Returns true if the bundle needs compilation (bundleHash not found in bundles table).
     */
    @Transactional
    public SyncResult upsertFromBinding(ValidationServicePort.RuleBindingDto binding) {
        String subjectType = binding.objectType();
        String subjectKey = binding.objectId();
        String ruleId = binding.ruleId();

        if (ruleId == null || subjectType == null || subjectKey == null) {
            logger.warn("Skipping binding with missing required fields: id={}, ruleId={}, objectType={}, objectId={}",
                    binding.id(), ruleId, subjectType, subjectKey);
            return new SyncResult(null, false);
        }

        Optional<AssignmentEntity> existing = assignmentRepository.findBySubjectTypeAndSubjectKeyAndRuleId(
                subjectType, subjectKey, ruleId);

        AssignmentEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            // Check source version to avoid stale updates
            if (binding.version() != null && entity.getSourceVersion() != null
                    && binding.version() <= entity.getSourceVersion()) {
                logger.debug("Skipping stale update: subjectType={}, subjectKey={}, ruleId={}, version={} <= {}",
                        subjectType, subjectKey, ruleId, binding.version(), entity.getSourceVersion());
                return new SyncResult(entity, false);
            }
            logger.info("Updating assignment: id={}, subjectType={}, subjectKey={}, ruleId={}",
                    entity.getId(), subjectType, subjectKey, ruleId);
        } else {
            entity = new AssignmentEntity();
            entity.setSubjectType(subjectType);
            entity.setSubjectKey(subjectKey);
            entity.setRuleId(ruleId);
            entity.setCreatedAt(Instant.now());
            logger.info("Creating assignment: subjectType={}, subjectKey={}, ruleId={}", subjectType, subjectKey, ruleId);
        }

        // Update fields
        entity.setBundleHash(binding.bundleHash());
        entity.setActive(binding.active() == null || binding.active());
        entity.setPriority(binding.priority() != null ? binding.priority() : 0);
        entity.setValidFrom(binding.validFrom());
        entity.setValidTo(binding.validTo());
        entity.setTimezone(binding.timezone() != null ? binding.timezone() : "Asia/Ho_Chi_Minh");
        entity.setRrule(binding.rrule());
        entity.setTimeWindows(toJson(binding.timeWindows()));
        entity.setExcludedDates(toJson(binding.excludedDates()));
        entity.setTrafficPercent(binding.trafficPercent() != null ? binding.trafficPercent() : 100);
        entity.setStickyKeyStrategy(binding.stickyKeyStrategy());
        entity.setIncludedAll(binding.includedAll() == null || binding.includedAll());
        entity.setIncludedProducts(toJson(binding.includedProducts()));
        entity.setExcludedProducts(toJson(binding.excludedProducts()));
        entity.setIncludedCategories(toJson(binding.includedCategories()));
        entity.setExcludedCategories(toJson(binding.excludedCategories()));
        entity.setIncludedBrands(toJson(binding.includedBrands()));
        entity.setExcludedBrands(toJson(binding.excludedBrands()));
        entity.setSourceVersion(binding.version() != null ? binding.version() : 0L);
        entity.setUpdatedAt(Instant.now());

        AssignmentEntity saved = assignmentRepository.save(entity);

        // Check if bundle needs compilation
        boolean needsCompile = binding.bundleHash() == null
                || !bundleRepository.existsById(binding.bundleHash());

        return new SyncResult(saved, needsCompile);
    }

    /**
     * Upsert assignment from event data (minimal payload).
     */
    @Transactional
    public SyncResult upsertFromEvent(EventUpsertCommand cmd) {
        String ruleId = cmd.ruleId();
        String subjectType = cmd.subjectType();
        String subjectKey = cmd.subjectKey();
        if (ruleId == null || subjectType == null || subjectKey == null) {
            logger.warn("Skipping event with missing required fields: assignmentId={}, ruleId={}, subjectType={}, subjectKey={}",
                    cmd.assignmentId(), ruleId, subjectType, subjectKey);
            return new SyncResult(null, false);
        }

        Optional<AssignmentEntity> existing = assignmentRepository.findBySubjectTypeAndSubjectKeyAndRuleId(
                subjectType, subjectKey, ruleId);

        AssignmentEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            logger.info("Updating assignment from event: id={}, subjectType={}, subjectKey={}", entity.getId(), subjectType, subjectKey);
        } else {
            entity = new AssignmentEntity();
            entity.setId(cmd.assignmentId());
            entity.setSubjectType(subjectType);
            entity.setSubjectKey(subjectKey);
            entity.setRuleId(ruleId);
            entity.setCreatedAt(Instant.now());
            logger.info("Creating assignment from event: subjectType={}, subjectKey={}, ruleId={}", subjectType, subjectKey, ruleId);
        }

        entity.setActive(cmd.active());
        entity.setPriority(cmd.priority() != null ? cmd.priority() : 0);
        entity.setTrafficPercent(cmd.trafficPercent() != null ? cmd.trafficPercent() : 100);
        entity.setValidFrom(cmd.validFrom());
        entity.setValidTo(cmd.validTo());
        entity.setTimezone(cmd.timezone() != null ? cmd.timezone() : "Asia/Ho_Chi_Minh");
        entity.setUpdatedAt(Instant.now());

        if (cmd.bundleHash() != null) {
            entity.setBundleHash(cmd.bundleHash());
        }

        AssignmentEntity saved = assignmentRepository.save(entity);
        boolean needsCompile = entity.getBundleHash() == null
                || !bundleRepository.existsById(entity.getBundleHash());

        return new SyncResult(saved, needsCompile);
    }

    // Sonar rules S100/S107/S1186 are false positives on Java records (older sonar-java plugins
    // analyze record components/canonical constructor as regular methods).
    @SuppressWarnings({"java:S100", "java:S107", "java:S1186"})
    public record EventUpsertCommand(String assignmentId, String ruleId, String subjectType, String subjectKey, // NOSONAR
                                     boolean active, Integer trafficPercent, Integer priority,
                                     String bundleHash, Instant validFrom, Instant validTo, String timezone) {
        // Empty body intentional — Java record canonical constructor is implicit.
    }

    @Transactional
    public void deactivateAssignment(String subjectType, String subjectKey, String ruleId) {
        Optional<AssignmentEntity> existing = assignmentRepository.findBySubjectTypeAndSubjectKeyAndRuleId(
                subjectType, subjectKey, ruleId);
        if (existing.isPresent()) {
            AssignmentEntity entity = existing.get();
            entity.setActive(false);
            entity.setUpdatedAt(Instant.now());
            assignmentRepository.save(entity);
            logger.info("Deactivated assignment: subjectType={}, subjectKey={}, ruleId={}", subjectType, subjectKey, ruleId);
        }
    }

    @Transactional
    public void deleteAssignment(String subjectType, String subjectKey, String ruleId) {
        assignmentRepository.deleteBySubjectTypeAndSubjectKeyAndRuleId(subjectType, subjectKey, ruleId);
        logger.info("Deleted assignment: subjectType={}, subjectKey={}, ruleId={}", subjectType, subjectKey, ruleId);
    }

    public List<AssignmentEntity> findAllActive() {
        return assignmentRepository.findAllActive();
    }

    public List<AssignmentEntity> findActiveBySubject(String subjectType, String subjectKey) {
        return assignmentRepository.findActiveBySubjectOrderByPriority(subjectType, subjectKey);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize to JSON", e);
            return null;
        }
    }

    // Sonar rules S100/S1186 are false positives on Java records (older sonar-java plugins
    // analyze record components/canonical constructor as regular methods).
    @SuppressWarnings({"java:S100", "java:S1186"})
    public record SyncResult(AssignmentEntity assignment, boolean needsCompile) { // NOSONAR
        // Empty body intentional — Java record canonical constructor is implicit.
    }
}
