package vn.viettel.vds.promotion.validation.engine.adapter.out.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.application.port.out.SessionLockPort;
import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionLockAdapter implements SessionLockPort {

    private static final Logger logger = LoggerFactory.getLogger(SessionLockAdapter.class);
    private static final String LOCK_PREFIX = "validation:lock:";

    private final RedisTemplate<String, String> redisTemplate;
    // Fallback in-memory locks for development when Redis is not available
    private final ConcurrentHashMap<String, Long> inMemoryLocks = new ConcurrentHashMap<>();

    public SessionLockAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquireValidationLock(Candidate candidate, String customerId, int ttlSeconds) {
        String lockKey = generateLockKey(candidate, customerId);

        try {
            // Try Redis first
            if (redisTemplate != null) {
                return acquireRedisLock(lockKey, ttlSeconds);
            } else {
                return acquireInMemoryLock(lockKey, ttlSeconds);
            }
        } catch (Exception e) {
            logger.warn("Failed to acquire Redis lock for {}, falling back to in-memory", lockKey, e);
            return acquireInMemoryLock(lockKey, ttlSeconds);
        }
    }

    @Override
    public boolean releaseValidationLock(Candidate candidate, String customerId) {
        String lockKey = generateLockKey(candidate, customerId);

        try {
            if (redisTemplate != null) {
                return releaseRedisLock(lockKey);
            } else {
                return releaseInMemoryLock(lockKey);
            }
        } catch (Exception e) {
            logger.warn("Failed to release Redis lock for {}, falling back to in-memory", lockKey, e);
            return releaseInMemoryLock(lockKey);
        }
    }

    @Override
    public boolean isValidationLocked(Candidate candidate, String customerId) {
        String lockKey = generateLockKey(candidate, customerId);

        try {
            if (redisTemplate != null) {
                return checkRedisLock(lockKey);
            } else {
                return checkInMemoryLock(lockKey);
            }
        } catch (Exception e) {
            logger.warn("Failed to check Redis lock for {}, falling back to in-memory", lockKey, e);
            return checkInMemoryLock(lockKey);
        }
    }

    @Override
    public boolean extendValidationLock(Candidate candidate, String customerId, int ttlSeconds) {
        String lockKey = generateLockKey(candidate, customerId);

        try {
            if (redisTemplate != null) {
                return extendRedisLock(lockKey, ttlSeconds);
            } else {
                return extendInMemoryLock(lockKey, ttlSeconds);
            }
        } catch (Exception e) {
            logger.warn("Failed to extend Redis lock for {}, falling back to in-memory", lockKey, e);
            return extendInMemoryLock(lockKey, ttlSeconds);
        }
    }

    private String generateLockKey(Candidate candidate, String customerId) {
        String candidateId = candidate.getCode() != null ? candidate.getCode() : candidate.getId();
        return LOCK_PREFIX + candidate.getType() + ":" + candidateId + ":" + customerId;
    }

    // Redis lock operations
    private boolean acquireRedisLock(String lockKey, int ttlSeconds) {
        try {
            String lockValue = String.valueOf(System.currentTimeMillis());
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(ttlSeconds));

            boolean result = acquired != null && acquired;
            if (result) {
                logger.debug("Acquired Redis lock: {}", lockKey);
            } else {
                logger.debug("Failed to acquire Redis lock: {}", lockKey);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error acquiring Redis lock: {}", lockKey, e);
            return false;
        }
    }

    private boolean releaseRedisLock(String lockKey) {
        try {
            Boolean deleted = redisTemplate.delete(lockKey);
            boolean result = deleted != null && deleted;
            if (result) {
                logger.debug("Released Redis lock: {}", lockKey);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error releasing Redis lock: {}", lockKey, e);
            return false;
        }
    }

    private boolean checkRedisLock(String lockKey) {
        try {
            Boolean exists = redisTemplate.hasKey(lockKey);
            return exists != null && exists;
        } catch (Exception e) {
            logger.error("Error checking Redis lock: {}", lockKey, e);
            return false;
        }
    }

    private boolean extendRedisLock(String lockKey, int ttlSeconds) {
        try {
            Boolean extended = redisTemplate.expire(lockKey, Duration.ofSeconds(ttlSeconds));
            boolean result = extended != null && extended;
            if (result) {
                logger.debug("Extended Redis lock: {}", lockKey);
            }
            return result;
        } catch (Exception e) {
            logger.error("Error extending Redis lock: {}", lockKey, e);
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