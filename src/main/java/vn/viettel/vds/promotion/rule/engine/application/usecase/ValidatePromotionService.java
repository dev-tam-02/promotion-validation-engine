package vn.viettel.vds.promotion.rule.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.application.dto.ValidationRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.ValidationResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.in.ValidatePromotionUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RulesServicePort;
import vn.viettel.vds.promotion.rule.engine.application.port.out.SessionLockPort;
import vn.viettel.vds.promotion.rule.engine.application.service.CounterResult;
import vn.viettel.vds.promotion.rule.engine.application.service.QuotaCounterService;
import vn.viettel.vds.promotion.rule.engine.domain.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ValidatePromotionService implements ValidatePromotionUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ValidatePromotionService.class);

    private static final String MESSAGE_KEY = "message";

    private final RuleEnginePort ruleEnginePort;
    private final RulesServicePort rulesServicePort;
    private final SessionLockPort sessionLockPort;
    private final QuotaCounterService quotaCounterService;

    public ValidatePromotionService(@Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort,
                                    RulesServicePort rulesServicePort,
                                    SessionLockPort sessionLockPort,
                                    QuotaCounterService quotaCounterService) {
        this.ruleEnginePort = ruleEnginePort;
        this.rulesServicePort = rulesServicePort;
        this.sessionLockPort = sessionLockPort;
        this.quotaCounterService = quotaCounterService;
    }

    @Override
    public ValidationResponse validatePromotions(ValidationRequest request) {
        List<Decision> decisions = new ArrayList<>();

        // Early validation checks that can fail fast
        if (!isValidRequest(request)) {
            Decision errorDecision = createInvalidDecision(null,
                    List.of(new ReasonCode("INVALID_REQUEST", Map.of(MESSAGE_KEY, "Invalid request structure"))));
            return new ValidationResponse(List.of(errorDecision));
        }

        // Check for short-circuit conditions based on order or customer
        if (shouldShortCircuit(request)) {
            return createShortCircuitResponse(request);
        }

        // Process each candidate with early exit optimization
        for (Candidate candidate : request.candidates()) {
            Decision decision = validateSingleCandidate(request, candidate);
            decisions.add(decision);

            // Optional: Early exit if critical validation fails
            if (isCriticalFailure(decision)) {
                logger.warn("Critical validation failure for candidate: {}, stopping further validation",
                        candidate.getCode());
                break;
            }
        }

        return new ValidationResponse(decisions);
    }

    private boolean isValidRequest(ValidationRequest request) {
        return request != null &&
                request.customer() != null &&
                request.order() != null &&
                request.candidates() != null &&
                !request.candidates().isEmpty();
    }

    private boolean shouldShortCircuit(ValidationRequest request) {
        // Example short-circuit conditions

        // Check minimum order amount
        if (request.order().getTotal() != null && request.order().getTotal().doubleValue() < 10000) { // 10k minimum
            logger.info("Short-circuiting validation: order amount {} below minimum",
                    request.order().getTotal());
            return true;
        }

        // Check customer status (if customer has blocked status)
        if (request.customer().getSegments() != null &&
                request.customer().getSegments().contains("BLOCKED")) {
            logger.info("Short-circuiting validation: customer {} is blocked",
                    request.customer().getId());
            return true;
        }

        return false;
    }

    private ValidationResponse createShortCircuitResponse(ValidationRequest request) {
        List<Decision> decisions = new ArrayList<>();

        for (Candidate candidate : request.candidates()) {
            List<ReasonCode> reasons = new ArrayList<>();

            if (request.order().getTotal() != null && request.order().getTotal().doubleValue() < 10000) {
                reasons.add(new ReasonCode("ORDER_AMOUNT_TOO_LOW",
                        Map.of("minimum", 10000, "actual", request.order().getTotal().doubleValue())));
            }

            if (request.customer().getSegments() != null &&
                    request.customer().getSegments().contains("BLOCKED")) {
                reasons.add(new ReasonCode("CUSTOMER_BLOCKED"));
            }

            decisions.add(createInvalidDecision(candidate, reasons));
        }

        return new ValidationResponse(decisions);
    }

    private boolean isCriticalFailure(Decision decision) {
        // Define what constitutes a critical failure that should stop processing
        if (decision.getReasons() != null) {
            for (ReasonCode reason : decision.getReasons()) {
                if ("VALIDATION_ERROR".equals(reason.getCode()) ||
                        "RULE_BUNDLE_NOT_FOUND".equals(reason.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ReasonCode> collectReasonsForFailure(boolean finalValid, ValidationResult ruleResult) {
        List<ReasonCode> reasons = new ArrayList<>();
        if (finalValid) {
            return reasons;
        }
        if (ruleResult.getReasonCodes() != null) {
            ruleResult.getReasonCodes().forEach(rc -> reasons.add(new ReasonCode(rc)));
        } else if (ruleResult.getMessage() != null) {
            reasons.add(new ReasonCode("RULE_FAILED", Map.of(MESSAGE_KEY, ruleResult.getMessage())));
        }
        return reasons;
    }

    private Decision validateSingleCandidate(ValidationRequest request, Candidate candidate) {
        String customerId = request.customer().getId();
        boolean lockAcquired = false;

        try {
            // 1. Pre-check: basic candidate validation
            if (!isValidCandidate(candidate)) {
                return createInvalidDecision(candidate, List.of(new ReasonCode("CANDIDATE_INVALID")));
            }

            // 2. Try to acquire session lock to prevent double validation
            lockAcquired = sessionLockPort.acquireValidationLock(candidate, customerId, 120); // 2 minutes TTL
            if (!lockAcquired) {
                logger.info("Validation session locked for candidate: {} and customer: {}",
                        candidate.getCode(), customerId);
                return createInvalidDecision(candidate,
                        List.of(new ReasonCode("VALIDATION_SESSION_LOCKED",
                                Map.of(MESSAGE_KEY, "Another validation is in progress for this candidate"))));
            }

            // 2. Get rule bundle from rules repository
            RulesServicePort.RuleBundle ruleBundle = rulesServicePort.getRuleBundle(candidate);
            if (ruleBundle == null) {
                return createInvalidDecision(candidate, List.of(new ReasonCode("RULE_BUNDLE_NOT_FOUND")));
            }

            // 3. Load rule bundle into Drools engine if not already loaded
            if (!ruleEnginePort.isRuleBundleLoaded(ruleBundle.getBundleHash())) {
                ruleEnginePort.loadRuleBundle(ruleBundle.getBundleHash(), ruleBundle.getKieModuleBytes());
            }

            // 4. Execute Drools rules
            ValidationResult ruleResult = ruleEnginePort.executeRules(
                    request.customer(), request.order(), candidate, ruleBundle.getBundleHash());

            // 5. Post-eval: counter quota enforcement (Phase-1)
            if (ruleResult.hasPolicies() && "ALLOW".equals(ruleResult.getDecision())) {
                enforceQuotaPolicies(ruleResult, ruleBundle.getBundleHash());
            }

            // 7. Compose final decision based on rule execution (verdict may have been updated by quota enforcement)
            boolean finalValid = ruleResult.isMatched() && !"DENY".equals(ruleResult.getDecision());
            List<ReasonCode> reasons = collectReasonsForFailure(finalValid, ruleResult);

            Decision decision = new Decision(candidate, finalValid);
            decision.setReasons(reasons);
            return decision;

        } catch (Exception e) {
            return createInvalidDecision(candidate,
                    List.of(new ReasonCode("VALIDATION_ERROR", Map.of("error", e.getMessage()))));
        } finally {
            // Always release the session lock
            if (lockAcquired) {
                boolean released = sessionLockPort.releaseValidationLock(candidate, customerId);
                if (released) {
                    logger.debug("Released validation session lock for candidate: {} and customer: {}",
                            candidate.getCode(), customerId);
                } else {
                    logger.warn("Failed to release validation session lock for candidate: {} and customer: {}",
                            candidate.getCode(), customerId);
                }
            }
        }
    }

    /**
     * Phase-1 quota policy enforcement: for each emitted {@link QuotaPolicy},
     * atomically increments the counter. If any limit is exceeded, rolls back all
     * previously incremented counters and flips the verdict to DENY.
     *
     * @param result the {@link ValidationResult} whose verdict may be mutated
     * @param ruleId rule identifier used as Redis key namespace
     */
    void enforceQuotaPolicies(ValidationResult result, String ruleId) {
        List<QuotaPolicy> incremented = new ArrayList<>();

        for (QuotaPolicy policy : result.getPolicies()) {
            CounterResult cr = quotaCounterService.incrementWithCheck(
                    ruleId, policy.getBucketKey(), policy.getLimit());

            if (cr.isOk()) {
                incremented.add(policy);
            } else {
                // Limit exceeded — roll back all previously incremented counters
                logger.info("[QuotaEnforce] Policy {} exceeded — current={}, limit={}, rolling back {} prior increment(s)",
                        policy.getPolicyName(), cr.getCurrent(), policy.getLimit(), incremented.size());
                for (QuotaPolicy prev : incremented) {
                    quotaCounterService.decrement(ruleId, prev.getBucketKey());
                }
                // Flip verdict to DENY
                result.setDecision("DENY");
                result.setOk(false);
                result.addReasonCode("QUOTA_EXCEEDED_" + policy.getPolicyName().toUpperCase());
                return;
            }
        }
        logger.debug("[QuotaEnforce] All {} policy(ies) passed for ruleId={}", result.getPolicies().size(), ruleId);
    }

    private boolean isValidCandidate(Candidate candidate) {
        return candidate != null &&
                candidate.getType() != null &&
                (candidate.getCode() != null || candidate.getId() != null);
    }


    private Decision createInvalidDecision(Candidate candidate, List<ReasonCode> reasons) {
        Decision decision = new Decision(candidate, false);
        decision.setReasons(reasons);
        return decision;
    }
}