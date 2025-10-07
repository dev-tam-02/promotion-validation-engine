package vn.viettel.vds.promotion.validation.engine.domain.service.compilation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.domain.service.DroolsCompilationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class CachedCompilationService {

    private static final Logger logger = LoggerFactory.getLogger(CachedCompilationService.class);

    private final DroolsCompilationService compilationService;
    private final CompilationCacheService cacheService;
    private final CompilationCacheConfig cacheConfig;

    public CachedCompilationService(DroolsCompilationService compilationService,
                                    CompilationCacheService cacheService,
                                    CompilationCacheConfig cacheConfig) {
        this.compilationService = compilationService;
        this.cacheService = cacheService;
        this.cacheConfig = cacheConfig;
    }

    public DroolsCompilationService.CompilationResult compileDrl(String tenantId, String ruleId,
                                                                 Integer version, String drlContent,
                                                                 String operatorsFingerprint) {
        logger.debug("Compiling DRL with caching for rule: {}", ruleId);

        // Check cache if enabled
        if (cacheConfig.isEnabled() && operatorsFingerprint != null) {
            String structureHash = generateStructureHash(drlContent);
            String cacheKey = cacheService.generateCacheKey(ruleId, operatorsFingerprint, structureHash);

            CompilationCacheService.CachedCompilation cached = cacheService.getOrCompile(cacheKey, () -> {
                logger.debug("Cache miss - performing compilation for rule: {}", ruleId);
                DroolsCompilationService.CompilationResult result = compilationService.compileDrl(tenantId, ruleId, version, drlContent);
                return convertToCompilationResult(result, drlContent);
            });

            return convertFromCachedResult(cached);
        }

        // No cache, compile directly
        logger.debug("Cache disabled or no fingerprint - compiling directly for rule: {}", ruleId);
        return compilationService.compileDrl(tenantId, ruleId, version, drlContent);
    }

    public void invalidateCache(String ruleId) {
        if (cacheConfig.isEnabled()) {
            // Invalidate all cache entries for this rule
            cacheService.invalidateByPattern(ruleId + ":.*");
        }
    }

    public void clearCache() {
        if (cacheConfig.isEnabled()) {
            cacheService.clear();
        }
    }

    public CompilationCacheService.CompilationCacheStats getCacheStats() {
        if (cacheConfig.isEnabled()) {
            return cacheService.getStats();
        }
        return null;
    }

    private String generateStructureHash(String drlContent) {
        try {
            // Generate hash based on DRL structure (normalize whitespace and comments)
            String normalizedDrl = normalizeDrlForHashing(drlContent);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedDrl.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String normalizeDrlForHashing(String drlContent) {
        // Remove comments and normalize whitespace for consistent hashing
        return drlContent
                .replaceAll("//.*", "")           // Remove single-line comments
                .replaceAll("/\\*.*?\\*/", "")    // Remove multi-line comments
                .replaceAll("\\s+", " ")          // Normalize whitespace
                .trim();
    }

    private CompilationCacheService.CompilationResult convertToCompilationResult(
            DroolsCompilationService.CompilationResult original, String drlContent) {

        long compilationTime = 0; // We don't have timing info from original
        return CompilationCacheService.CompilationResult.success(
                original.getBundleHash(),
                original.getArtifactBytes(),
                drlContent,
                compilationTime
        );
    }

    private DroolsCompilationService.CompilationResult convertFromCachedResult(
            CompilationCacheService.CachedCompilation cached) {

        CompilationCacheService.CompilationResult result = cached.getResult();

        return new DroolsCompilationService.CompilationResult(
                result.getBundleHash(),
                result.getCompiledBytes(),
                (long) result.getCompiledBytes().length,
                java.util.List.of("Served from compilation cache"),
                "cached"
        );
    }
}