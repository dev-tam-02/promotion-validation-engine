package vn.viettel.vds.promotion.rule.engine.application.dto;

import java.time.Instant;
import java.util.List;

public record CompileJobResponse(
        String id,
        String tenantId,
        String ruleId,
        Integer targetVersion,
        String status,
        String requestedBy,
        Instant requestedAt,
        Instant completedAt,
        String bundleHash,
        List<LogEntry> logs,
        List<String> errors
) {
    public record LogEntry(
            String level,
            String msg,
            Instant timestamp
    ) {
    }
}