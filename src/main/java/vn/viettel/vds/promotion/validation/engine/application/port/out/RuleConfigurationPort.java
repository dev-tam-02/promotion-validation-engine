package vn.viettel.vds.promotion.validation.engine.application.port.out;

import vn.viettel.vds.promotion.validation.engine.domain.model.RuleConfiguration;

public interface RuleConfigurationPort {

    RuleConfiguration getConfiguration(String tenantId, String campaignId);
}