package vn.viettel.vds.promotion.rule.engine.adapter.in.web;

import com.promix.platform.web.annotation.ResponseWrapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.viettel.vds.promotion.rule.engine.application.service.ReconciliationService;

import java.util.Map;

@RestController
@ResponseWrapper
@RequestMapping("${spring.application.context-path}/v1/admin")
public class SyncController {

    private final ReconciliationService reconciliationService;

    public SyncController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/sync")
    public Map<String, Object> triggerSync() {
        ReconciliationService.SyncStats stats = reconciliationService.triggerSync();
        return Map.of(
                "total", stats.getTotal(),
                "created", stats.getCreated(),
                "updated", stats.getUpdated(),
                "needsCompile", stats.getNeedsCompile(),
                "failed", stats.getFailed(),
                "staleRemoved", stats.getStaleRemoved()
        );
    }
}
