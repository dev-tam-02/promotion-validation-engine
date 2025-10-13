package vn.viettel.vds.promotion.validation.engine.application.usecase;

import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.application.port.in.ManageRulesUseCase;

@Service
public class ManageRulesService implements ManageRulesUseCase {

    public ManageRulesService() {
        // Default constructor
    }

    @Override
    public void reloadRules() {
        // Rule reloading is now handled by bundle loading mechanism
        // This could trigger a cache invalidation if needed
    }
}