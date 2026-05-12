package vn.viettel.vds.promotion.rule.engine.application.port.out;

import vn.viettel.vds.promotion.rule.engine.domain.model.RuleConfiguration;

public interface RuleConfigurationPort {

    /**
     * Load the fast-check configuration for a subject.
     *
     * @param subjectType canonical subject type (e.g. {@code DISCOUNT_COUPON})
     * @param subjectKey  canonical subject key (e.g. the resolved campaignId)
     * @return the loaded configuration, or {@code null} if no row is
     * configured for the subject — callers must fail-closed.
     */
    RuleConfiguration getConfiguration(String subjectType, String subjectKey);
}
