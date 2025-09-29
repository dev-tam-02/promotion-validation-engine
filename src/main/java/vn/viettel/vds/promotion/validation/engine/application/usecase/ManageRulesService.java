package vn.viettel.vds.promotion.validation.engine.application.usecase;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.application.port.in.ManageRulesUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleEnginePort;

@Service
public class ManageRulesService implements ManageRulesUseCase {

    private final RuleEnginePort ruleEnginePort;

    public ManageRulesService(@Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort) {
        this.ruleEnginePort = ruleEnginePort;
    }

    @Override
    public void reloadRules() {
        // Rule reloading is now handled by bundle loading mechanism
        // This could trigger a cache invalidation if needed
    }
}