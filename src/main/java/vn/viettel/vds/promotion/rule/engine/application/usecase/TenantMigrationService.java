package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.config.TenantConfiguration;

@Service
public class TenantMigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantMigrationService.class);
    
    private final TenantConfiguration tenantConfig;
    
    public TenantMigrationService(TenantConfiguration tenantConfig) {
        this.tenantConfig = tenantConfig;
    }
    
    /**
     * Get effective tenant ID for operations
     * During migration period, this handles backward compatibility
     */
    public String getEffectiveTenantId(String requestTenantId) {
        if (tenantConfig.isSingleTenantMode()) {
            if (requestTenantId != null && !requestTenantId.equals(tenantConfig.getDefaultId())) {
                logger.warn("Ignoring tenantId '{}' in single-tenant mode, using default: '{}'", 
                        requestTenantId, tenantConfig.getDefaultId());
            }
            return tenantConfig.getDefaultId();
        }
        
        return requestTenantId != null ? requestTenantId : tenantConfig.getDefaultId();
    }
    
    /**
     * Check if tenant migration is enabled
     */
    public boolean isMigrationEnabled() {
        return tenantConfig.isEnableTenantMigration();
    }
    
    /**
     * Get default package name for DRL generation
     */
    public String getDefaultPackageName() {
        return sanitizePackageName(tenantConfig.getDefaultId());
    }
    
    private String sanitizePackageName(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return "defaultpackage";
        }
        
        return tenantId.toLowerCase()
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("^[0-9]", "_$0")
                .replaceAll("_{2,}", "_");
    }
}
