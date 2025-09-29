package vn.viettel.vds.promotion.validation.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.OutboxEvent;
import vn.viettel.vds.promotion.validation.engine.application.port.out.EventPublisherPort;
import vn.viettel.vds.promotion.validation.engine.application.port.out.OutboxEventRepositoryPort;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OutboxEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventProcessor.class);
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 50;

    @Autowired
    private OutboxEventRepositoryPort outboxEventRepository;

    @Autowired
    private EventPublisherPort eventPublisher;

    @Scheduled(fixedDelay = 5000) // Process events every 5 seconds
    @Transactional
    public void processOutboxEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxEventRepository
                    .findByStatusOrderByCreatedAt(OutboxEvent.EventStatus.PENDING);

            if (pendingEvents.isEmpty()) {
                return;
            }

            logger.info("Processing {} pending outbox events", pendingEvents.size());

            for (OutboxEvent event : pendingEvents.subList(0, Math.min(pendingEvents.size(), BATCH_SIZE))) {
                processEvent(event);
            }

        } catch (Exception e) {
            logger.error("Error processing outbox events", e);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            // Update attempt count
            event.setAttempts(event.getAttempts() + 1);
            event.setLastTriedAt(Instant.now());

            // Publish event based on type
            switch (event.getType()) {
                case BUNDLE_PUBLISHED:
                    publishBundlePublishedEvent(event);
                    break;
                case WARMUP_REQUESTED:
                    publishWarmupRequestedEvent(event);
                    break;
                default:
                    logger.warn("Unknown event type: {}", event.getType());
                    return;
            }

            // Mark as successfully sent
            event.setStatus(OutboxEvent.EventStatus.SENT);
            outboxEventRepository.save(event);

            logger.debug("Successfully processed outbox event: {}", event.getId());

        } catch (Exception e) {
            logger.error("Failed to process outbox event: {}", event.getId(), e);

            // Mark as failed if max attempts reached
            if (event.getAttempts() >= MAX_RETRY_ATTEMPTS) {
                event.setStatus(OutboxEvent.EventStatus.FAILED);
                logger.error("Outbox event {} failed after {} attempts", event.getId(), MAX_RETRY_ATTEMPTS);
            }

            outboxEventRepository.save(event);
        }
    }

    private void publishBundlePublishedEvent(OutboxEvent event) {
        Map<String, Object> payload = event.getPayload();

        EventPublisherPort.BundlePublishedEvent bundleEvent = new EventPublisherPort.BundlePublishedEvent(
                event.getTenantId(),
                (String) payload.get("ruleId"),
                (Integer) payload.get("ruleVersion"),
                (Integer) payload.get("assignmentVersion"),
                (String) payload.get("bundleHash")
        );

        eventPublisher.publishBundlePublished(bundleEvent);
    }

    private void publishWarmupRequestedEvent(OutboxEvent event) {
        Map<String, Object> payload = event.getPayload();

        EventPublisherPort.WarmupRequestedEvent warmupEvent = new EventPublisherPort.WarmupRequestedEvent(
                event.getTenantId(),
                (String) payload.get("bundleHash")
        );

        eventPublisher.publishWarmupRequested(warmupEvent);
    }

    @Scheduled(fixedDelay = 3600000) // Clean up every hour
    @Transactional
    public void cleanupProcessedEvents() {
        try {
            // Delete successfully sent events older than 24 hours
            Instant cutoff = Instant.now().minusSeconds(24 * 60 * 60);

            List<OutboxEvent> oldEvents = outboxEventRepository
                    .findByStatusOrderByCreatedAt(OutboxEvent.EventStatus.SENT);

            List<String> eventIdsToDelete = oldEvents.stream()
                    .filter(event -> event.getCreatedAt().isBefore(cutoff))
                    .map(OutboxEvent::getId)
                    .toList();

            if (!eventIdsToDelete.isEmpty()) {
                outboxEventRepository.deleteByIdIn(eventIdsToDelete);
                logger.info("Cleaned up {} processed outbox events", eventIdsToDelete.size());
            }

        } catch (Exception e) {
            logger.error("Error cleaning up outbox events", e);
        }
    }
}