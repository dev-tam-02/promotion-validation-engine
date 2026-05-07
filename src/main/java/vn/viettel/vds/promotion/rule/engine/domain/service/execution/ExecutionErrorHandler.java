package vn.viettel.vds.promotion.rule.engine.domain.service.execution;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.exception.RuleExecutionException;
import vn.viettel.vds.promotion.rule.engine.domain.exception.SessionCreationException;

@Component
public class ExecutionErrorHandler {

    public void handleExecutionError(String executionId, String bundleHash, Exception e) {
        if (e instanceof SessionCreationException) {
            throw new RuleExecutionException(
                    "Failed to create session for rule execution",
                    bundleHash,
                    executionId,
                    e
            );
        }

        throw new RuleExecutionException(
                "Rule execution failed: " + e.getMessage(),
                bundleHash,
                executionId,
                e
        );
    }

    public boolean isRecoverableError(Exception e) {
        return e instanceof SessionCreationException;
    }

    public void attemptRecovery(Exception e) {
        // Implement recovery strategies like cache invalidation, session pool reset, etc.
        if (e instanceof SessionCreationException) {
            // Could trigger bundle reload or session pool cleanup
        }
    }
}
