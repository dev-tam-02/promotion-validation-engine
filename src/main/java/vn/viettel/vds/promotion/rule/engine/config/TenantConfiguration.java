package vn.viettel.vds.promotion.rule.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rule-engine.tenant")
public class TenantConfiguration {
    
    private String defaultId = "default";
    private boolean singleTenantMode = true;
    private boolean enableTenantMigration = false;
    
    public String getDefaultId() {
        return defaultId;
    }
    
    public void setDefaultId(String defaultId) {
        this.defaultId = defaultId;
    }
    
    public boolean isSingleTenantMode() {
        return singleTenantMode;
    }
    
    public void setSingleTenantMode(boolean singleTenantMode) {
        this.singleTenantMode = singleTenantMode;
    }
    
    public boolean isEnableTenantMigration() {
        return enableTenantMigration;
    }
    
    public void setEnableTenantMigration(boolean enableTenantMigration) {
        this.enableTenantMigration = enableTenantMigration;
    }
}
