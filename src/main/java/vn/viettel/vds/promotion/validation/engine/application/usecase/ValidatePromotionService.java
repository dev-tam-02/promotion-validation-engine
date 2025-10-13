package vn.viettel.vds.promotion.validation.engine.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.validation.engine.application.dto.ValidationRequest;
import vn.viettel.vds.promotion.validation.engine.application.dto.ValidationResponse;
import vn.viettel.vds.promotion.validation.engine.application.port.in.ValidatePromotionUseCase;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RuleEnginePort;
import vn.viettel.vds.promotion.validation.engine.application.port.out.RulesServicePort;
import vn.viettel.vds.promotion.validation.engine.application.port.out.SessionLockPort;
import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;
import vn.viettel.vds.promotion.validation.engine.domain.model.Decision;
import vn.viettel.vds.promotion.validation.engine.domain.model.ReasonCode;
import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;

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

    public ValidatePromotionService(@Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort,
                                    RulesServicePort rulesServicePort,
                                    SessionLockPort sessionLockPort) {
        this.ruleEnginePort = ruleEnginePort;
        this.rulesServicePort = rulesServicePort;
        this.sessionLockPort = sessionLockPort;
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
        if (request.order().getTotalAmount() < 10000) { // 10k minimum
            logger.info("Short-circuiting validation: order amount {} below minimum",
                    request.order().getTotalAmount());
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

            if (request.order().getTotalAmount() < 10000) {
                reasons.add(new ReasonCode("ORDER_AMOUNT_TOO_LOW",
                        Map.of("minimum", 10000, "actual", request.order().getTotalAmount())));
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

            // 5. Compose final decision based only on rule execution
            boolean finalValid = ruleResult.isMatched();
            List<ReasonCode> reasons = new ArrayList<>();

            if (!ruleResult.isMatched()) {
                reasons.add(new ReasonCode("RULE_FAILED", Map.of(MESSAGE_KEY, ruleResult.getMessage())));
            }

            // 6. Create decision
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