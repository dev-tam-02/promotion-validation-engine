package vn.viettel.vds.promotion.validation.engine.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.validation.engine.application.port.in.ManageRulesUseCase;

@RestController
@RequestMapping("/api/rules")
public class RuleAdminController {

    private final ManageRulesUseCase manageRulesUseCase;

    public RuleAdminController(ManageRulesUseCase manageRulesUseCase) {
        this.manageRulesUseCase = manageRulesUseCase;
    }

    @PostMapping("/reload")
    public ResponseEntity<String> reload() {
        manageRulesUseCase.reloadRules();
        return ResponseEntity.ok("Rules reloaded");
    }
}