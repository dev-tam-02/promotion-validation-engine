package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.domain.model.RuleConfiguration;

public interface RuleConfigurationPort {

    RuleConfiguration getConfiguration(String tenantId, String campaignId);
}