package vn.viettel.vds.promotion.rule.engine.application.dto;

import java.time.Instant;
import java.util.List;

// Sonar rules S100/S107/S1172/S1186 are false positives on Java records (older sonar-java plugins
// analyze record components/canonical constructor as regular methods with too many/unused params).
@SuppressWarnings({"java:S100", "java:S107", "java:S1172", "java:S1186"})
public record CompileJobResponse( // NOSONAR
        String id,
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
    @SuppressWarnings({"java:S100", "java:S1172", "java:S1186"})
    public record LogEntry( // NOSONAR
            String level,
            String msg,
            Instant timestamp
    ) {
        // Empty body intentional — Java record canonical constructor is implicit.
    }
}