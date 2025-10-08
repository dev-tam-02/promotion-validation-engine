package vn.viettel.vds.promotion.validation.engine.adapter.out.cache;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.application.port.out.SessionLockPort;
import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class SessionLockAdapter implements SessionLockPort {

    private static final Logger logger = LoggerFactory.getLogger(SessionLockAdapter.class);
    private static final String LOCK_PREFIX = "validation:lock:";

    private final RedissonClient redissonClient;
    // Fallback in-memory locks for development when Redisson is not available
    private final ConcurrentHashMap<String, Long> inMemoryLocks = new ConcurrentHashMap<>();

    public SessionLockAdapter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean acquireValidationLock(Candidate candidate, String customerId, int ttlSeconds) {
        String lockKey = generateLockKey(candidate, customerId);

        try {
            // Try Redisson first
            if (redissonClient != null) {
                return acquireRedissonLock(lockKey, ttlSeconds);
            } else {
                return acquireInMemoryLock(lockKey, ttlSeconds);
            }
        } catch (Exception e) {
            logger.warn("Failed to acquire Redisson lock for {}, falling back to in-memory", lockKey, e);
            return acquireInMemoryLock(lockKey, ttlSeconds);
        }
    }

    @Override
    public boolean releaseValidationLock(Candidate candidate, String customerId) {
        String lockKey = generateLockKey(candidate, customerId);

        try {
            if (redissonClient != null) {
                return releaseRedissonLock(lockKey);
            } else {
                return releaseInMemoryLock(lockKey);
            }
        } catch (Exception e) {
            logger.warn("Failed to release Redisson lock for {}, falling back to in-memory", lockKey, e);
            return releaseInMemoryLock(lockKey);
        }
    }

    @Override
    public boolean isValidationLocked(Candidate candidate, String customerId) {
        String lockKey = generateLockKey(candidate, customerId);

        try {
            if (redissonClient != null) {
                return checkRedissonLock(lockKey);
            } else {
                return checkInMemoryLock(lockKey);
            }
        } catch (Exception e) {
            logger.warn("Failed to check Redisson lock for {}, falling back to in-memory", lockKey, e);
            return checkInMemoryLock(lockKey);
        }
    }

    @Override
    public boolean extendValidationLock(Candidate candidate, String customerId, int ttlSeconds) {
        String lockKey = generateLockKey(candidate, customerId);

        try {
            if (redissonClient != null) {
                return extendRedissonLock(lockKey, ttlSeconds);
            } else {
                return extendInMemoryLock(lockKey, ttlSeconds);
            }
        } catch (Exception e) {
            logger.warn("Failed to extend Redisson lock for {}, falling back to in-memory", lockKey, e);
            return extendInMemoryLock(lockKey, ttlSeconds);
        }
    }

    private String generateLockKey(Candidate candidate, String customerId) {
        String candidateId = candidate.getCode() != null ? candidate.getCode() : candidate.getId();
        return LOCK_PREFIX + candidate.getType() + ":" + candidateId + ":" + customerId;
    }

    // Redisson lock operations using RLock
    private boolean acquireRedissonLock(String lockKey, int ttlSeconds) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean acquired = lock.tryLock(0, ttlSeconds, TimeUnit.SECONDS);

            if (acquired) {
                logger.debug("Acquired Redisson lock: {}", lockKey);
            } else {
                logger.debug("Failed to acquire Redisson lock: {}", lockKey);
            }
            return acquired;
        } catch (Exception e) {
            logger.error("Error acquiring Redisson lock: {}", lockKey, e);
            return false;
        }
    }

    private boolean releaseRedissonLock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                logger.debug("Released Redisson lock: {}", lockKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error releasing Redisson lock: {}", lockKey, e);
            return false;
        }
    }

    private boolean checkRedissonLock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            return lock.isLocked();
        } catch (Exception e) {
            logger.error("Error checking Redisson lock: {}", lockKey, e);
            return false;
        }
    }

    private boolean extendRedissonLock(String lockKey, int ttlSeconds) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            // Redisson doesn't have direct extend - we need to re-acquire with new TTL
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                boolean reacquired = lock.tryLock(0, ttlSeconds, TimeUnit.SECONDS);
                if (reacquired) {
                    logger.debug("Extended Redisson lock: {}", lockKey);
                }
                return reacquired;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error extending Redisson lock: {}", lockKey, e);
            return false;
        }
    }

    // In-memory fallback lock operations
    private boolean acquireInMemoryLock(String lockKey, int ttlSeconds) {
        long expirationTime = System.currentTimeMillis() + (ttlSeconds * 1000L);
        Long existing = inMemoryLocks.putIfAbsent(lockKey, expirationTime);

        if (existing == null) {
            logger.debug("Acquired in-memory lock: {}", lockKey);
            return true;
        }

        // Check if existing lock has expired
        if (existing < System.currentTimeMillis()) {
            inMemoryLocks.put(lockKey, expirationTime);
            logger.debug("Acquired expired in-memory lock: {}", lockKey);
            return true;
        }

        logger.debug("Failed to acquire in-memory lock: {}", lockKey);
        return false;
    }

    private boolean releaseInMemoryLock(String lockKey) {
        Long removed = inMemoryLocks.remove(lockKey);
        boolean result = removed != null;
        if (result) {
            logger.debug("Released in-memory lock: {}", lockKey);
        }
        return result;
    }

    private boolean checkInMemoryLock(String lockKey) {
        Long expiration = inMemoryLocks.get(lockKey);
        if (expiration == null) {
            return false;
        }

        // Check if lock has expired
        if (expiration < System.currentTimeMillis()) {
            inMemoryLocks.remove(lockKey);
            return false;
        }

        return true;
    }

    private boolean extendInMemoryLock(String lockKey, int ttlSeconds) {
        Long existing = inMemoryLocks.get(lockKey);
        if (existing == null) {
            return false;
        }

        long newExpiration = System.currentTimeMillis() + (ttlSeconds * 1000L);
        inMemoryLocks.put(lockKey, newExpiration);
        logger.debug("Extended in-memory lock: {}", lockKey);
        return true;
    }
}