package vn.viettel.vds.promotion.validation.engine.domain.service.bundle;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BundleRepository {

    // This is a placeholder implementation
    // In a real implementation, this would query the database for active rules

    public List<ActiveRuleInfo> findActiveRules() {
        // TODO: Implement database query to find all active rules with their compiled bundles
        // For now, return empty list to prevent compilation errors
        return List.of();
    }

    public ActiveRuleInfo findActiveRule(String ruleId) {
        // TODO: Implement database query to find specific active rule
        // For now, return null to prevent compilation errors
        return null;
    }
}