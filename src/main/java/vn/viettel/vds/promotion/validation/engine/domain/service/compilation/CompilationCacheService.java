package vn.viettel.vds.promotion.validation.engine.domain.service.compilation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(value = "validation.engine.compilation.cache-enabled", havingValue = "true", matchIfMissing = true)
public class CompilationCacheService {

    private static final Logger logger = LoggerFactory.getLogger(CompilationCacheService.class);

    private final Cache<String, CachedCompilation> compilationCache;
    private final CompilationCacheConfig config;

    // Metrics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong compilationTime = new AtomicLong(0);

    public CompilationCacheService(CompilationCacheConfig config) {
        this.config = config;
        this.compilationCache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .expireAfterWrite(Duration.ofMinutes(config.getTtlMinutes()))
                .recordStats()
                .removalListener((key, value, cause) -> {
                    logger.debug("Compilation cache entry removed: key={}, cause={}", key, cause);
                    if (value instanceof CachedCompilation cached) {
                        cached.cleanup();
                    }
                })
                .build();

        logger.info("Compilation cache initialized: maxSize={}, ttlMinutes={}",
                config.getMaxSize(), config.getTtlMinutes());
    }

    public CachedCompilation getOrCompile(String cacheKey, CompilationSupplier supplier) {
        logger.debug("Checking compilation cache for key: {}", cacheKey);

        CachedCompilation cached = compilationCache.getIfPresent(cacheKey);

        if (cached != null && cached.isValid()) {
            cacheHits.incrementAndGet();
            logger.debug("Compilation cache hit for key: {}", cacheKey);
            return cached;
        }

        // Cache miss - perform compilation
        cacheMisses.incrementAndGet();
        logger.debug("Compilation cache miss for key: {}", cacheKey);

        long startTime = System.currentTimeMillis();
        try {
            CompilationResult result = supplier.compile();
            long duration = System.currentTimeMillis() - startTime;
            compilationTime.addAndGet(duration);

            cached = new CachedCompilation(result, cacheKey);
            compilationCache.put(cacheKey, cached);

            logger.debug("Compilation completed and cached: key={}, duration={}ms", cacheKey, duration);
            return cached;

        } catch (Exception e) {
            throw new CompilationException(
                    String.format("Compilation failed for key: %s", cacheKey), e);
        }
    }

    public void invalidate(String cacheKey) {
        logger.debug("Invalidating compilation cache entry: {}", cacheKey);
        compilationCache.invalidate(cacheKey);
    }

    public void invalidateByPattern(String pattern) {
        logger.debug("Invalidating compilation cache entries by pattern: {}", pattern);

        compilationCache.asMap().keySet().stream()
                .filter(key -> key.matches(pattern))
                .forEach(this::invalidate);
    }

    public void clear() {
        logger.info("Clearing compilation cache");
        compilationCache.invalidateAll();
    }

    public CompilationCacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = compilationCache.stats();

        return new CompilationCacheStats(
                compilationCache.estimatedSize(),
                cacheHits.get(),
                cacheMisses.get(),
                stats.hitRate(),
                compilationTime.get(),
                stats.averageLoadPenalty() / 1_000_000, // Convert to milliseconds
                stats.evictionCount()
        );
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public String generateCacheKey(String ruleId, String operatorsFingerprint, String nodeStructureHash) {
        return String.format("%s:%s:%s", ruleId, operatorsFingerprint, nodeStructureHash);
    }

    @FunctionalInterface
    public interface CompilationSupplier {
        CompilationResult compile() throws CompilationException;
    }

    // Inner classes
    public static class CachedCompilation {
        private final CompilationResult result;
        private final String cacheKey;
        private final Instant cachedAt;

        public CachedCompilation(CompilationResult result, String cacheKey) {
            this.result = result;
            this.cacheKey = cacheKey;
            this.cachedAt = Instant.now();
        }

        public CompilationResult getResult() {
            return result;
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public Instant getCachedAt() {
            return cachedAt;
        }

        public boolean isValid() {
            return result != null && result.isSuccess();
        }

        public void cleanup() {
            // Cleanup any resources if needed
            if (result != null) {
                result.cleanup();
            }
        }
    }

    public static class CompilationResult {
        private final boolean success;
        private final String bundleHash;
        private final byte[] compiledBytes;
        private final String drlContent;
        private final String errorMessage;
        private final long compilationTimeMs;

        private CompilationResult(boolean success, String bundleHash, byte[] compiledBytes,
                                  String drlContent, String errorMessage, long compilationTimeMs) {
            this.success = success;
            this.bundleHash = bundleHash;
            this.compiledBytes = compiledBytes;
            this.drlContent = drlContent;
            this.errorMessage = errorMessage;
            this.compilationTimeMs = compilationTimeMs;
        }

        public static CompilationResult success(String bundleHash, byte[] compiledBytes, String drlContent, long compilationTimeMs) {
            return new CompilationResult(true, bundleHash, compiledBytes, drlContent, null, compilationTimeMs);
        }

        public static CompilationResult failure(String errorMessage, long compilationTimeMs) {
            return new CompilationResult(false, null, null, null, errorMessage, compilationTimeMs);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getBundleHash() {
            return bundleHash;
        }

        public byte[] getCompiledBytes() {
            return compiledBytes;
        }

        public String getDrlContent() {
            return drlContent;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getCompilationTimeMs() {
            return compilationTimeMs;
        }

        public void cleanup() {
            // Cleanup resources if needed
        }
    }

    public static class CompilationCacheStats {
        private final long size;
        private final long hits;
        private final long misses;
        private final double hitRate;
        private final long totalCompilationTimeMs;
        private final double averageCompilationTimeMs;
        private final long evictions;

        public CompilationCacheStats(long size, long hits, long misses, double hitRate,
                                     long totalCompilationTimeMs, double averageCompilationTimeMs, long evictions) {
            this.size = size;
            this.hits = hits;
            this.misses = misses;
            this.hitRate = hitRate;
            this.totalCompilationTimeMs = totalCompilationTimeMs;
            this.averageCompilationTimeMs = averageCompilationTimeMs;
            this.evictions = evictions;
        }

        // Getters
        public long getSize() {
            return size;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public double getHitRate() {
            return hitRate;
        }

        public long getTotalCompilationTimeMs() {
            return totalCompilationTimeMs;
        }

        public double getAverageCompilationTimeMs() {
            return averageCompilationTimeMs;
        }

        public long getEvictions() {
            return evictions;
        }
    }
}