package vn.viettel.vds.promotion.validation.engine.domain.service.compilation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "validation.engine.compilation")
public class CompilationCacheConfig {

    private boolean cacheEnabled = true;
    private int maxSize = 100;
    private int ttlMinutes = 120;
    private boolean structureHashEnabled = true;
    private boolean operatorFingerprintEnabled = true;

    // Getters and setters
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public boolean isEnabled() {
        return cacheEnabled;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(int ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public boolean isStructureHashEnabled() {
        return structureHashEnabled;
    }

    public void setStructureHashEnabled(boolean structureHashEnabled) {
        this.structureHashEnabled = structureHashEnabled;
    }

    public boolean isOperatorFingerprintEnabled() {
        return operatorFingerprintEnabled;
    }

    public void setOperatorFingerprintEnabled(boolean operatorFingerprintEnabled) {
        this.operatorFingerprintEnabled = operatorFingerprintEnabled;
    }
}