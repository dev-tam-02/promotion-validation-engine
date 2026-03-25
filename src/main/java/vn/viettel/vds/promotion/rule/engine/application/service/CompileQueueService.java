package vn.viettel.vds.promotion.rule.engine.application.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.out.persistence.jpa.entity.CompileJobEntity;
import vn.viettel.vds.promotion.rule.engine.application.dto.CompileRequest;
import vn.viettel.vds.promotion.rule.engine.application.port.in.CompileUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.CompileJobRepositoryPort;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CompileQueueService {

    private static final Logger logger = LoggerFactory.getLogger(CompileQueueService.class);
    private static final String COMPILE_LOCK_PREFIX = "compile:lock:";
    private static final long LOCK_LEASE_SECONDS = 300;
    private static final Duration STALE_JOB_THRESHOLD = Duration.ofMinutes(30);

    private final RedissonClient redissonClient;
    private final CompileUseCase compileUseCase;
    private final CompileJobRepositoryPort compileJobRepository;

    public CompileQueueService(RedissonClient redissonClient,
                               CompileUseCase compileUseCase,
                               CompileJobRepositoryPort compileJobRepository) {
        this.redissonClient = redissonClient;
        this.compileUseCase = compileUseCase;
        this.compileJobRepository = compileJobRepository;
    }

    /**
     * Submit a compile request with distributed lock to prevent race conditions.
     * Returns true if compilation was triggered or already exists, false on failure.
     */
    public boolean submitCompile(CompileRequest request) {
        String ruleId = request.getRuleId();
        int version = request.getVersion();
        String lockKey = COMPILE_LOCK_PREFIX + ruleId + ":" + version;

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(0, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                logger.info("Compile lock held by another instance, skipping: ruleId={}, version={}", ruleId, version);
                return true;
            }

            // Check idempotency: already compiled?
            Optional<CompileJobEntity> existingJob = compileJobRepository.findByRuleIdAndTargetVersion(ruleId, version);
            if (existingJob.isPresent()) {
                CompileJobEntity job = existingJob.get();
                if (job.getStatus() == CompileJobEntity.JobStatus.SUCCESS) {
                    logger.info("Compile already successful, skipping: ruleId={}, version={}, bundleHash={}",
                            ruleId, version, job.getBundleHash());
                    return true;
                }
                if (job.getStatus() == CompileJobEntity.JobStatus.RUNNING) {
                    Duration age = Duration.between(job.getRequestedAt(), Instant.now());
                    if (age.compareTo(STALE_JOB_THRESHOLD) < 0) {
                        logger.info("Compile job still running, skipping: ruleId={}, version={}, age={}",
                                ruleId, version, age);
                        return true;
                    }
                    logger.warn("Stale RUNNING compile job detected, marking as FAILED: ruleId={}, version={}",
                            ruleId, version);
                    job.setStatus(CompileJobEntity.JobStatus.FAILED);
                    job.setCompletedAt(Instant.now());
                    job.getErrors().add("Timed out after " + STALE_JOB_THRESHOLD.toMinutes() + " minutes");
                    compileJobRepository.save(job);
                }
                // FAILED status: allow recompile by falling through
            }

            logger.info("Submitting compile: ruleId={}, version={}", ruleId, version);
            compileUseCase.compile(request);
            logger.info("Compile completed successfully: ruleId={}, version={}", ruleId, version);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while acquiring compile lock: ruleId={}, version={}", ruleId, version);
            return false;
        } catch (Exception e) {
            logger.error("Compile failed: ruleId={}, version={}", ruleId, version, e);
            return false;
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    logger.warn("Failed to release compile lock: ruleId={}, version={}", ruleId, version, e);
                }
            }
        }
    }
}
