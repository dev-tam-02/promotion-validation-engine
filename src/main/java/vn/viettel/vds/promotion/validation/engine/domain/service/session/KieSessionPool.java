package vn.viettel.vds.promotion.validation.engine.domain.service.session;

import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class KieSessionPool {

    private static final Logger logger = LoggerFactory.getLogger(KieSessionPool.class);

    private final ConcurrentHashMap<String, BundleSessionPool> pools = new ConcurrentHashMap<>();
    private final KieSessionFactory sessionFactory;
    private final SessionPoolConfig config;

    public KieSessionPool(KieSessionFactory sessionFactory, SessionPoolConfig config) {
        this.sessionFactory = sessionFactory;
        this.config = config;
    }

    public PooledKieSession borrowSession(String bundleHash) {
        BundleSessionPool pool = pools.computeIfAbsent(bundleHash,
            hash -> new BundleSessionPool(hash, sessionFactory, config));

        return pool.borrowSession();
    }

    public void returnSession(PooledKieSession pooledSession) {
        BundleSessionPool pool = pools.get(pooledSession.getBundleHash());
        if (pool != null) {
            pool.returnSession(pooledSession);
        } else {
            // Pool doesn't exist anymore, dispose the session
            pooledSession.dispose();
        }
    }

    public void invalidateBundle(String bundleHash) {
        BundleSessionPool pool = pools.remove(bundleHash);
        if (pool != null) {
            pool.dispose();
            logger.info("Invalidated session pool for bundle: {}", bundleHash);
        }
    }

    public PoolStatistics getStatistics(String bundleHash) {
        BundleSessionPool pool = pools.get(bundleHash);
        return pool != null ? pool.getStatistics() : null;
    }

    public void cleanup() {
        logger.info("Starting session pool cleanup");

        for (BundleSessionPool pool : pools.values()) {
            pool.cleanup();
        }

        // Remove empty pools
        pools.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        logger.info("Session pool cleanup completed. Active pools: {}", pools.size());
    }

    private static class BundleSessionPool {
        private final String bundleHash;
        private final KieSessionFactory sessionFactory;
        private final SessionPoolConfig config;
        private final ConcurrentLinkedQueue<PooledKieSession> availableSessions;
        private final AtomicInteger activeCount = new AtomicInteger(0);
        private final AtomicInteger totalCreated = new AtomicInteger(0);
        private final AtomicInteger totalBorrowed = new AtomicInteger(0);
        private final AtomicInteger totalReturned = new AtomicInteger(0);
        private final ReentrantLock lock = new ReentrantLock();
        private volatile Instant lastAccessTime = Instant.now();

        public BundleSessionPool(String bundleHash, KieSessionFactory sessionFactory, SessionPoolConfig config) {
            this.bundleHash = bundleHash;
            this.sessionFactory = sessionFactory;
            this.config = config;
            this.availableSessions = new ConcurrentLinkedQueue<>();

            // Pre-warm the pool
            for (int i = 0; i < config.getCorePoolSize(); i++) {
                createNewSession();
            }
        }

        public PooledKieSession borrowSession() {
            lastAccessTime = Instant.now();
            totalBorrowed.incrementAndGet();

            PooledKieSession session = availableSessions.poll();

            if (session == null) {
                // No available session, create new one if under limit
                if (activeCount.get() < config.getMaxPoolSize()) {
                    session = createNewSession();
                } else {
                    // Pool exhausted, wait or throw exception
                    throw new SessionPoolExhaustedException(
                        "Session pool exhausted for bundle: " + bundleHash +
                        ", active: " + activeCount.get() +
                        ", max: " + config.getMaxPoolSize());
                }
            }

            session.markBorrowed();
            return session;
        }

        public void returnSession(PooledKieSession session) {
            totalReturned.incrementAndGet();

            if (session.isValid() && !session.isExpired(config.getSessionTtlMinutes())) {
                session.markReturned();
                availableSessions.offer(session);
            } else {
                // Session invalid or expired, dispose it
                session.dispose();
                activeCount.decrementAndGet();
            }
        }

        private PooledKieSession createNewSession() {
            lock.lock();
            try {
                if (activeCount.get() >= config.getMaxPoolSize()) {
                    throw new SessionPoolExhaustedException("Pool size limit reached: " + config.getMaxPoolSize());
                }

                KieSession kieSession = sessionFactory.createSession(bundleHash);
                PooledKieSession pooledSession = new PooledKieSession(kieSession, bundleHash);

                activeCount.incrementAndGet();
                totalCreated.incrementAndGet();

                return pooledSession;

            } finally {
                lock.unlock();
            }
        }

        public void cleanup() {
            Instant cutoffTime = Instant.now().minusSeconds(config.getIdleTimeoutMinutes() * 60L);

            int removedCount = 0;
            PooledKieSession session;

            while ((session = availableSessions.poll()) != null) {
                if (session.getLastUsedTime().isBefore(cutoffTime) || session.isExpired(config.getSessionTtlMinutes())) {
                    session.dispose();
                    activeCount.decrementAndGet();
                    removedCount++;
                } else {
                    // Session still valid, put it back
                    availableSessions.offer(session);
                    break; // Assuming sessions are ordered by last used time
                }
            }

            if (removedCount > 0) {
                logger.debug("Cleaned up {} expired sessions for bundle: {}", removedCount, bundleHash);
            }
        }

        public void dispose() {
            PooledKieSession session;
            while ((session = availableSessions.poll()) != null) {
                session.dispose();
            }
            activeCount.set(0);
        }

        public boolean isEmpty() {
            return activeCount.get() == 0;
        }

        public PoolStatistics getStatistics() {
            return new PoolStatistics(
                bundleHash,
                activeCount.get(),
                availableSessions.size(),
                totalCreated.get(),
                totalBorrowed.get(),
                totalReturned.get(),
                lastAccessTime
            );
        }
    }

    public static class PooledKieSession {
        private final KieSession kieSession;
        private final String bundleHash;
        private final Instant createdTime;
        private volatile Instant lastUsedTime;
        private volatile boolean borrowed;

        public PooledKieSession(KieSession kieSession, String bundleHash) {
            this.kieSession = kieSession;
            this.bundleHash = bundleHash;
            this.createdTime = Instant.now();
            this.lastUsedTime = Instant.now();
            this.borrowed = false;
        }

        public KieSession getKieSession() {
            return kieSession;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public Instant getCreatedTime() {
            return createdTime;
        }

        public Instant getLastUsedTime() {
            return lastUsedTime;
        }

        public boolean isBorrowed() {
            return borrowed;
        }

        public void markBorrowed() {
            this.borrowed = true;
            this.lastUsedTime = Instant.now();
        }

        public void markReturned() {
            this.borrowed = false;
            this.lastUsedTime = Instant.now();
        }

        public boolean isValid() {
            try {
                // Simple validation - check if session is still usable
                return kieSession != null && kieSession.getId() > 0;
            } catch (Exception e) {
                return false;
            }
        }

        public boolean isExpired(int ttlMinutes) {
            return createdTime.isBefore(Instant.now().minusSeconds(ttlMinutes * 60L));
        }

        public void dispose() {
            try {
                if (kieSession != null) {
                    kieSession.dispose();
                }
            } catch (Exception e) {
                logger.warn("Error disposing KIE session: {}", e.getMessage());
            }
        }
    }

    public static class PoolStatistics {
        private final String bundleHash;
        private final int activeCount;
        private final int availableCount;
        private final int totalCreated;
        private final int totalBorrowed;
        private final int totalReturned;
        private final Instant lastAccessTime;

        public PoolStatistics(String bundleHash, int activeCount, int availableCount,
                            int totalCreated, int totalBorrowed, int totalReturned,
                            Instant lastAccessTime) {
            this.bundleHash = bundleHash;
            this.activeCount = activeCount;
            this.availableCount = availableCount;
            this.totalCreated = totalCreated;
            this.totalBorrowed = totalBorrowed;
            this.totalReturned = totalReturned;
            this.lastAccessTime = lastAccessTime;
        }

        // Getters
        public String getBundleHash() { return bundleHash; }
        public int getActiveCount() { return activeCount; }
        public int getAvailableCount() { return availableCount; }
        public int getTotalCreated() { return totalCreated; }
        public int getTotalBorrowed() { return totalBorrowed; }
        public int getTotalReturned() { return totalReturned; }
        public Instant getLastAccessTime() { return lastAccessTime; }
        public double getHitRatio() {
            return totalBorrowed > 0 ? (double)(totalBorrowed - totalCreated) / totalBorrowed : 0.0;
        }
    }

    public static class SessionPoolExhaustedException extends RuntimeException {
        public SessionPoolExhaustedException(String message) {
            super(message);
        }
    }
}