package vn.viettel.vds.promotion.rule.engine.domain.service.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "validation.engine.session-pool")
public class SessionPoolConfig {

    private int corePoolSize = 5;
    private int maxPoolSize = 20;
    private int sessionTtlMinutes = 60;
    private int idleTimeoutMinutes = 30;
    private int cleanupIntervalMinutes = 15;
    private boolean enablePooling = true;
    private boolean preWarmPool = true;

    // Getters and setters
    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getSessionTtlMinutes() {
        return sessionTtlMinutes;
    }

    public void setSessionTtlMinutes(int sessionTtlMinutes) {
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public int getIdleTimeoutMinutes() {
        return idleTimeoutMinutes;
    }

    public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
        this.idleTimeoutMinutes = idleTimeoutMinutes;
    }

    public int getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }

    public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;
    }

    public boolean isEnablePooling() {
        return enablePooling;
    }

    public void setEnablePooling(boolean enablePooling) {
        this.enablePooling = enablePooling;
    }

    public boolean isPreWarmPool() {
        return preWarmPool;
    }

    public void setPreWarmPool(boolean preWarmPool) {
        this.preWarmPool = preWarmPool;
    }
}