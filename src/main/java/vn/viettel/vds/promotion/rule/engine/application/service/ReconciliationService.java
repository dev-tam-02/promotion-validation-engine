package vn.viettel.vds.promotion.rule.engine.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.AssignmentEntity;
import vn.viettel.vds.promotion.rule.engine.application.port.out.AssignmentRepositoryPort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.ValidationServicePort;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    private final ValidationServicePort validationService;
    private final AssignmentSyncService assignmentSyncService;
    private final AssignmentRepositoryPort assignmentRepository;
    private final AtomicBoolean startupCompleted = new AtomicBoolean(false);
    @Value("${validation.sync.startup-enabled:true}")
    private boolean startupSyncEnabled;
    @Value("${validation.sync.reconciliation-enabled:true}")
    private boolean reconciliationEnabled;
    @Value("${validation.sync.page-size:100}")
    private int pageSize;

    public ReconciliationService(ValidationServicePort validationService,
                                 AssignmentSyncService assignmentSyncService,
                                 AssignmentRepositoryPort assignmentRepository) {
        this.validationService = validationService;
        this.assignmentSyncService = assignmentSyncService;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Startup bulk sync: fetch all active bindings from pp-validation and sync locally.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!startupSyncEnabled) {
            logger.info("Startup sync disabled");
            startupCompleted.set(true);
            return;
        }

        logger.info("Starting startup sync with pp-validation...");
        try {
            SyncStats stats = performFullSync();
            logger.info("Startup sync completed: synced={}, created={}, updated={}, needsCompile={}",
                    stats.total, stats.created, stats.updated, stats.needsCompile);
        } catch (Exception e) {
            logger.error("Startup sync failed, will rely on event-driven sync and reconciliation", e);
        } finally {
            startupCompleted.set(true);
        }
    }

    /**
     * Periodic reconciliation: runs every 15 minutes (offset from refresh-ahead).
     * Detects and fixes drift between pp-validation and local assignment store.
     */
    @Scheduled(fixedDelayString = "${validation.sync.reconciliation-interval-ms:900000}",
            initialDelayString = "${validation.sync.reconciliation-initial-delay-ms:450000}")
    public void reconcile() {
        if (!reconciliationEnabled || !startupCompleted.get()) {
            return;
        }

        logger.info("Starting periodic reconciliation...");
        try {
            SyncStats stats = performFullSync();
            cleanupStaleAssignments(stats);
            logger.info("Reconciliation completed: synced={}, created={}, updated={}, needsCompile={}, staleRemoved={}",
                    stats.total, stats.created, stats.updated, stats.needsCompile, stats.staleRemoved);
        } catch (Exception e) {
            logger.error("Reconciliation failed", e);
        }
    }

    /**
     * Manual sync trigger.
     */
    public SyncStats triggerSync() {
        logger.info("Manual sync triggered");
        SyncStats stats = performFullSync();
        cleanupStaleAssignments(stats);
        return stats;
    }

    private SyncStats performFullSync() {
        SyncStats stats = new SyncStats();
        int page = 0;
        boolean lastPage = false;

        while (!lastPage) {
            List<ValidationServicePort.RuleBindingDto> bindings =
                    validationService.fetchActiveBindings(page, pageSize);

            if (bindings.isEmpty() || bindings.size() < pageSize) {
                lastPage = true;
            }

            for (ValidationServicePort.RuleBindingDto binding : bindings) {
                processBinding(binding, stats);
            }

            page++;
        }

        return stats;
    }

    private void processBinding(ValidationServicePort.RuleBindingDto binding, SyncStats stats) {
        try {
            if (binding.objectType() == null || binding.objectId() == null) {
                logger.debug("Skipping binding with null objectType/objectId: id={}", binding.id());
                return;
            }

            if (binding.ruleId() == null) {
                // Track subject pair so we don't deactivate event-created assignments
                // (Kafka consumer uses assignmentId as fallback ruleId)
                stats.remoteSubjectPairs.add(binding.objectType() + ":" + binding.objectId());
                logger.debug("Binding has null ruleId, tracking subject pair: {}:{}", binding.objectType(), binding.objectId());
                return;
            }

            String compositeKey = binding.objectType() + ":" + binding.objectId() + ":" + binding.ruleId();
            stats.remoteIds.add(compositeKey);

            AssignmentSyncService.SyncResult result = assignmentSyncService.upsertFromBinding(binding);
            if (result.assignment() == null) {
                return;
            }
            stats.total++;

            if (result.assignment().getCreatedAt() != null
                    && result.assignment().getCreatedAt().equals(result.assignment().getUpdatedAt())) {
                stats.created++;
            } else {
                stats.updated++;
            }

            if (result.needsCompile()) {
                stats.needsCompile++;
            }
        } catch (Exception e) {
            logger.error("Failed to sync binding: id={}, ruleId={}", binding.id(), binding.ruleId(), e);
            stats.failed++;
        }
    }

    private void cleanupStaleAssignments(SyncStats stats) {
        if (stats.remoteIds.isEmpty() && stats.remoteSubjectPairs.isEmpty()) {
            return;
        }

        List<AssignmentEntity> localActive = assignmentRepository.findAllActive();
        int removed = 0;

        for (AssignmentEntity local : localActive) {
            if (shouldKeep(local, stats)) {
                continue;
            }
            local.setActive(false);
            assignmentRepository.save(local);
            removed++;
            logger.info("Deactivated stale assignment: subjectType={}, subjectKey={}, ruleId={}",
                    local.getSubjectType(), local.getSubjectKey(), local.getRuleId());
        }

        stats.staleRemoved = removed;
    }

    private boolean shouldKeep(AssignmentEntity local, SyncStats stats) {
        String compositeKey = local.getSubjectType() + ":" + local.getSubjectKey() + ":" + local.getRuleId();
        String subjectPair = local.getSubjectType() + ":" + local.getSubjectKey();

        if (stats.remoteIds.contains(compositeKey)) {
            return true;
        }
        if (stats.remoteSubjectPairs.contains(subjectPair)) {
            logger.debug("Keeping event-created assignment for subject pair: {}, ruleId={}",
                    subjectPair, local.getRuleId());
            return true;
        }
        return false;
    }

    public static class SyncStats {
        int total;
        int created;
        int updated;
        int needsCompile;
        int failed;
        int staleRemoved;
        Set<String> remoteIds = new HashSet<>();
        Set<String> remoteSubjectPairs = new HashSet<>();

        public int getTotal() {
            return total;
        }

        public int getCreated() {
            return created;
        }

        public int getUpdated() {
            return updated;
        }

        public int getNeedsCompile() {
            return needsCompile;
        }

        public int getFailed() {
            return failed;
        }

        public int getStaleRemoved() {
            return staleRemoved;
        }
    }
}
