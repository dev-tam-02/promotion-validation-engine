package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleConfigurationPort;
import vn.viettel.vds.promotion.validation.engine.domain.model.RuleConfiguration;
import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationRuleConfig;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class RuleConfigurationAdapter implements RuleConfigurationPort {

    @Override
    public RuleConfiguration getConfiguration(String tenantId, String campaignId) {
        log.debug("Getting rule configuration for tenant: {} and campaign: {}", tenantId, campaignId);

        // For now, returning a default configuration
        // In a real implementation, this would fetch from a database or configuration service
        ValidationRuleConfig config = createDefaultConfiguration(tenantId, campaignId);

        return new RuleConfiguration(config);
    }

    private ValidationRuleConfig createDefaultConfiguration(String tenantId, String campaignId) {
        ValidationRuleConfig config = new ValidationRuleConfig();
        config.setRuleId("default-rule-" + campaignId);
        config.setCampaignId(campaignId);
        config.setTenantId(tenantId);
        config.setEnabled(true);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        // Create default fast check rules
        ValidationRuleConfig.FastCheckRules fastCheckRules = new ValidationRuleConfig.FastCheckRules();

        // Set default business hours (9 AM to 9 PM)
        ValidationRuleConfig.BusinessHours businessHours = new ValidationRuleConfig.BusinessHours();
        businessHours.setStartTime(LocalTime.of(9, 0));
        businessHours.setEndTime(LocalTime.of(21, 0));
        businessHours.setTimezone("Asia/Ho_Chi_Minh");
        fastCheckRules.setBusinessHours(businessHours);

        // Set allowed days of week (all days by default)
        fastCheckRules.setAllowedDaysOfWeek(Set.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"));

        // Set order constraints
        fastCheckRules.setMinOrderValue(100000L); // 100,000 VND minimum
        fastCheckRules.setMaxOrderValue(100000000L); // 100,000,000 VND maximum
        fastCheckRules.setAllowedCurrencies(Set.of("VND"));
        fastCheckRules.setMinItems(1);
        fastCheckRules.setMaxItems(100);

        // Set rate limits
        fastCheckRules.setMaxPerHour(10);
        fastCheckRules.setMaxPerDay(50);
        fastCheckRules.setMaxPerCustomer(100);

        config.setFastCheckRules(fastCheckRules);

        return config;
    }
}