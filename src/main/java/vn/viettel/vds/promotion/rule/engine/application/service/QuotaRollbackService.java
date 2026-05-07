package vn.viettel.vds.promotion.rule.engine.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.QuotaEventEntity;
import vn.viettel.vds.promotion.rule.engine.application.port.out.QuotaEventPort;

import java.util.List;

/**
 * Rolls back Layer-1 Redis counters for a given redemption.
 *
 * <p>For each uncompensated INCR/DECR event tied to the redemption ID, a corresponding
 * DECR (Redis) + ROLLBACK quota_event is written so the counter returns to its
 * pre-redemption value.
 */
@Service
@Transactional
public class QuotaRollbackService {

    private static final Logger log = LoggerFactory.getLogger(QuotaRollbackService.class);

    private final QuotaEventPort quotaEventPort;
    private final QuotaCounterService quotaCounterService;

    public QuotaRollbackService(QuotaEventPort quotaEventPort,
                                QuotaCounterService quotaCounterService) {
        this.quotaEventPort = quotaEventPort;
        this.quotaCounterService = quotaCounterService;
    }

    /**
     * Compensate all Layer-1 counter increments for the given {@code redemptionId}.
     *
     * @return number of counter entries reverted
     */
    public int rollback(String redemptionId) {
        List<QuotaEventEntity> uncompensated = quotaEventPort.findUncompensated(redemptionId);
        log.info("QuotaRollback: redemptionId={} uncompensatedCount={}", redemptionId, uncompensated.size());

        for (QuotaEventEntity event : uncompensated) {
            quotaCounterService.decrement(
                    event.getRuleId(),
                    redemptionId,
                    event.getCustomerId(),
                    event.getBucketKey(),
                    event.getWindowStart(),
                    event.getWindowEnd()
            );
            log.debug("QuotaRollback: reverted ruleId={} bucketKey={}", event.getRuleId(), event.getBucketKey());
        }

        return uncompensated.size();
    }
}
