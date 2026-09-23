package vn.viettel.vds.promotion.validation.engine.domain.service.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.ExecutionContextDto;
import vn.viettel.vds.promotion.validation.engine.domain.service.execution.FactPreparationService.ExecutionTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The evaluation time sent in executionContext.now must reach Drools as the ExecutionTimestamp fact (PROM-366).
 */
class FactPreparationServiceEvaluationTimeTest {

    private final FactPreparationService service = new FactPreparationService();

    @Test
    @DisplayName("Uses executionContext.now as the ExecutionTimestamp fact")
    void usesNowFromExecutionContext() {
        Instant transactionTime = Instant.parse("2026-09-14T01:59:00Z");
        Map<String, Object> context = new HashMap<>();
        context.put("executionContext", new ExecutionContextDto(transactionTime, "Asia/Ho_Chi_Minh"));

        List<ExecutionTimestamp> timestamps = timestamps(service.prepareFacts(context));

        assertEquals(1, timestamps.size());
        assertEquals(transactionTime.toEpochMilli(), timestamps.get(0).getTimestamp());
    }

    @Test
    @DisplayName("Falls back to the current time when no evaluation time is sent")
    void fallsBackToCurrentTime() {
        long before = System.currentTimeMillis();

        List<ExecutionTimestamp> timestamps = timestamps(service.prepareFacts(new HashMap<>()));

        assertEquals(1, timestamps.size());
        assertTrue(timestamps.get(0).getTimestamp() >= before);
    }

    private List<ExecutionTimestamp> timestamps(List<Object> facts) {
        return facts.stream()
                .filter(ExecutionTimestamp.class::isInstance)
                .map(ExecutionTimestamp.class::cast)
                .toList();
    }
}
