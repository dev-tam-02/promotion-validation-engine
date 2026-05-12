package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;

import java.util.List;

@Component
public class ExecutionResponseBuilder {

    private final ExecutionMetricsService metricsService;

    public ExecutionResponseBuilder(ExecutionMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public ExecuteResponse buildSuccessResponse(ValidationResult result,
                                                List<String> reasonCodes,
                                                long startTime,
                                                ExecutionTracingService.TracingAgendaEventListener tracingListener) {
        ExecuteResponse response = new ExecuteResponse();

        response.setOk(Boolean.TRUE.equals(result.getOk()));
        response.setDecision(result.getDecision() != null ? result.getDecision() : "DENY");
        response.setReasonCodes(determineReasonCodes(reasonCodes, result));
        response.setExplain(tracingListener != null ? tracingListener.getExplainEntries() : List.of());

        response.setEngine(buildEngineInfo(startTime));

        return response;
    }

    public ExecuteResponse buildErrorResponse(long startTime) {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        response.setReasonCodes(List.of("EXECUTION_ERROR"));
        response.setExplain(List.of(new ExecuteResponse.ExplainEntry("error", "exception", false)));
        response.setEngine(buildErrorEngineInfo(startTime));
        return response;
    }

    public ExecuteResponse buildBundleNotFoundResponse() {
        ExecuteResponse response = new ExecuteResponse();
        response.setOk(false);
        response.setDecision("DENY");
        response.setReasonCodes(List.of("BUNDLE_NOT_FOUND"));
        response.setExplain(List.of(new ExecuteResponse.ExplainEntry("bundle", "not_found", false)));
        response.setEngine(buildNotFoundEngineInfo());
        return response;
    }

    private List<String> determineReasonCodes(List<String> reasonCodes, ValidationResult result) {
        if (!reasonCodes.isEmpty()) {
            return reasonCodes;
        }
        return result.getReasonCodes() != null ? result.getReasonCodes() : List.of();
    }

    private ExecuteResponse.Engine buildEngineInfo(long startTime) {
        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion("drools-10.1.0");
        engine.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        engine.setCacheHit(metricsService != null); // Simplified for now
        return engine;
    }

    private ExecuteResponse.Engine buildErrorEngineInfo(long startTime) {
        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion("error");
        engine.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        engine.setCacheHit(false);
        return engine;
    }

    private ExecuteResponse.Engine buildNotFoundEngineInfo() {
        ExecuteResponse.Engine engine = new ExecuteResponse.Engine();
        engine.setVersion("N/A");
        engine.setLatencyMs(0);
        engine.setCacheHit(false);
        return engine;
    }

}
