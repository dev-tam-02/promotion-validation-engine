package vn.viettel.vds.promotion.rule.engine.domain.service.bundle;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "validation.engine.bundle")
public class BundlePreloadConfig {

    private boolean preloadEnabled = true;
    private boolean preloadOnStartup = false;
    private int preloadTimeoutMinutes = 10;
    private boolean prewarmSessionPool = true;
    private int prewarmSessionCount = 3;
    private int healthCheckIntervalSeconds = 300;

    // Getters and setters
    public boolean isPreloadEnabled() {
        return preloadEnabled;
    }

    public void setPreloadEnabled(boolean preloadEnabled) {
        this.preloadEnabled = preloadEnabled;
    }

    public boolean isPreloadOnStartup() {
        return preloadOnStartup;
    }

    public void setPreloadOnStartup(boolean preloadOnStartup) {
        this.preloadOnStartup = preloadOnStartup;
    }

    public int getPreloadTimeoutMinutes() {
        return preloadTimeoutMinutes;
    }

    public void setPreloadTimeoutMinutes(int preloadTimeoutMinutes) {
        this.preloadTimeoutMinutes = preloadTimeoutMinutes;
    }

    public boolean isPrewarmSessionPool() {
        return prewarmSessionPool;
    }

    public void setPrewarmSessionPool(boolean prewarmSessionPool) {
        this.prewarmSessionPool = prewarmSessionPool;
    }

    public int getPrewarmSessionCount() {
        return prewarmSessionCount;
    }

    public void setPrewarmSessionCount(int prewarmSessionCount) {
        this.prewarmSessionCount = prewarmSessionCount;
    }

    public int getHealthCheckIntervalSeconds() {
        return healthCheckIntervalSeconds;
    }

    public void setHealthCheckIntervalSeconds(int healthCheckIntervalSeconds) {
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
    }
}