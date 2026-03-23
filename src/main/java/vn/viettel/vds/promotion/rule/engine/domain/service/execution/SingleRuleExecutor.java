package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.domain.model.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SingleRuleExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SingleRuleExecutor.class);

    private final KieSessionManager sessionManager;
    private final FactPreparationService factPreparationService;
    private final ExecutionTracingService tracingService;
    private final ExecutionResponseBuilder responseBuilder;
    private final ExecutionErrorHandler errorHandler;

    public SingleRuleExecutor(KieSessionManager sessionManager,
                              FactPreparationService factPreparationService,
                              ExecutionTracingService tracingService,
                              ExecutionResponseBuilder responseBuilder,
                              ExecutionErrorHandler errorHandler) {
        this.sessionManager = sessionManager;
        this.factPreparationService = factPreparationService;
        this.tracingService = tracingService;
        this.responseBuilder = responseBuilder;
        this.errorHandler = errorHandler;
    }

    public ExecuteResponse execute(RuleEnginePort.ExecuteInput input, KieContainer container) {
        String executionId = generateExecutionId();
        long startTime = System.currentTimeMillis();

        try {
            StatelessKieSession session = sessionManager.createStatelessSession(container);
            
            ExecutionTracingService.TracingAgendaEventListener tracingListener = null;
            if (shouldEnableExplain(input.getOptions())) {
                tracingListener = tracingService.createTracingListener(executionId);
                session.addEventListener(tracingListener);
            }

            List<Object> facts = factPreparationService.prepareFacts(input.getContext());
            ValidationResult result = new ValidationResult();
            List<String> reasonCodes = new ArrayList<>();

            session.setGlobal("result", result);
            session.setGlobal("reasonCodes", reasonCodes);

            session.execute(facts);

            return responseBuilder.buildSuccessResponse(result, reasonCodes, startTime, tracingListener);

        } catch (Exception e) {
            logger.error("Single rule execution failed: executionId={}", executionId, e);
            
            // Attempt recovery if possible
            if (errorHandler.isRecoverableError(e)) {
                errorHandler.attemptRecovery(e);
            }
            
            // Handle the error appropriately
            errorHandler.handleExecutionError(executionId, input.getBundleHash(), e);
            return responseBuilder.buildErrorResponse(startTime);
        }
    }

    private boolean shouldEnableExplain(RuleEnginePort.ExecuteOptions options) {
        return options != null && options.getExplain() != null && !"NONE".equalsIgnoreCase(options.getExplain());
    }

    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
