package vn.viettel.vds.promotion.rule.engine.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;
import vn.viettel.vds.promotion.rule.engine.application.port.out.CompileJobRepositoryPort;

import java.time.Duration;
import java.time.Instant;

@Service
public class CompileJobWatchdog {

    private static final Logger logger = LoggerFactory.getLogger(CompileJobWatchdog.class);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(30);

    private final CompileJobRepositoryPort compileJobRepository;

    public CompileJobWatchdog(CompileJobRepositoryPort compileJobRepository) {
        this.compileJobRepository = compileJobRepository;
    }

    /**
     * Runs every 5 minutes to detect and clean up stale RUNNING compile jobs.
     * A job is considered stale if it has been RUNNING for more than 30 minutes.
     */
    @Scheduled(fixedDelayString = "${validation.sync.watchdog-interval-ms:300000}",
               initialDelayString = "${validation.sync.watchdog-initial-delay-ms:60000}")
    public void cleanupStaleJobs() {
        Instant cutoff = Instant.now().minus(STALE_THRESHOLD);
        int cleaned = 0;

        try {
            Page<CompileJobEntity> staleJobs = compileJobRepository.findByRuleIdAndStatusAndRequestedAtBetween(
                    null, CompileJobEntity.JobStatus.RUNNING,
                    Instant.EPOCH, cutoff,
                    PageRequest.of(0, 100));

            for (CompileJobEntity job : staleJobs.getContent()) {
                Duration age = Duration.between(job.getRequestedAt(), Instant.now());
                logger.warn("Found stale RUNNING compile job: jobId={}, ruleId={}, age={}",
                        job.getId(), job.getRuleId(), age);

                job.setStatus(CompileJobEntity.JobStatus.FAILED);
                job.setCompletedAt(Instant.now());
                job.getErrors().add("Watchdog timeout: job stuck in RUNNING state for " + age.toMinutes() + " minutes");
                compileJobRepository.save(job);
                cleaned++;
            }

            if (cleaned > 0) {
                logger.info("Watchdog cleaned {} stale compile jobs", cleaned);
            }
        } catch (Exception e) {
            logger.error("Watchdog failed to clean stale jobs", e);
        }
    }
}
