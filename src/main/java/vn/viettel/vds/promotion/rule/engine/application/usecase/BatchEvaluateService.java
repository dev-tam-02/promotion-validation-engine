package vn.viettel.vds.promotion.rule.engine.application.usecase;

import com.promix.platform.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.BatchEvaluateRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.BatchEvaluateResponse;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.BatchEvaluateResponse.SubjectResult;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.FastCheckRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.FastCheckResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.ExecuteResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.LatestBundleResponse;
import vn.viettel.vds.promotion.rule.engine.application.port.in.BatchEvaluateUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.in.BundleLookupUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.in.FastCheckUseCase;
import vn.viettel.vds.promotion.rule.engine.application.port.out.RuleEnginePort;

import java.math.BigDecimal;
import java.util.*;

/**
 * Orchestrates the single-hop redemption pipeline:
 * bundle resolve → fast-check → Drools execute → per-subject result.
 * <p>
 * Fail-closed at every stage: a missing rule, a fast-check denial, or a
 * Drools execution failure each produce an explicit DENY with a stable
 * reason code so callers never silently allow a redemption.
 */
@Service
@Slf4j
public class BatchEvaluateService implements BatchEvaluateUseCase {

    private final BundleLookupUseCase bundleLookupUseCase;
    private final FastCheckUseCase fastCheckUseCase;
    private final RuleEnginePort ruleEnginePort;

    public BatchEvaluateService(BundleLookupUseCase bundleLookupUseCase,
                                FastCheckUseCase fastCheckUseCase,
                                @Qualifier("droolsRuleEngineAdapter") RuleEnginePort ruleEnginePort) {
        this.bundleLookupUseCase = bundleLookupUseCase;
        this.fastCheckUseCase = fastCheckUseCase;
        this.ruleEnginePort = ruleEnginePort;
    }

    @Override
    public BatchEvaluateResponse evaluate(BatchEvaluateRequest request) {
        List<SubjectResult> results = new ArrayList<>(request.getSubjects().size());
        for (BatchEvaluateRequest.Subject subject : request.getSubjects()) {
            results.add(evaluateOne(request, subject));
        }
        return new BatchEvaluateResponse(results);
    }

    private SubjectResult evaluateOne(BatchEvaluateRequest request, BatchEvaluateRequest.Subject subject) {
        long started = System.currentTimeMillis();
        SubjectResult result = new SubjectResult();
        result.setSubjectType(subject.getSubjectType());
        result.setSubjectKey(subject.getSubjectKey());

        // Stage 1: resolve bundle — fail-closed on NO_RULE_CONFIGURED.
        LatestBundleResponse bundle;
        try {
            bundle = bundleLookupUseCase.getLatestBundle(subject.getSubjectType(), subject.getSubjectKey());
        } catch (ResourceNotFoundException e) {
            return deny(result, "BUNDLE_LOOKUP", "NO_RULE_CONFIGURED", e.getMessage(), started);
        } catch (Exception e) {
            log.error("Bundle lookup failed for {}:{}", subject.getSubjectType(), subject.getSubjectKey(), e);
            return deny(result, "BUNDLE_LOOKUP", "BUNDLE_LOOKUP_ERROR", e.getMessage(), started);
        }
        result.setBundleHash(bundle.getBundleHash());

        // Stage 2: fast-check gate.
        FastCheckRequest fastCheckRequest = toFastCheckRequest(request, subject);
        FastCheckResponse fastCheck;
        try {
            fastCheck = fastCheckUseCase.performFastCheck(fastCheckRequest);
        } catch (Exception e) {
            log.error("Fast-check failed for {}:{}", subject.getSubjectType(), subject.getSubjectKey(), e);
            return deny(result, "FAST_CHECK", "FAST_CHECK_ERROR", e.getMessage(), started);
        }
        if ("DENY".equals(fastCheck.getDecision())) {
            return deny(result, "FAST_CHECK",
                    fastCheck.getReasonCode() != null ? fastCheck.getReasonCode() : "FAST_CHECK_DENIED",
                    fastCheck.getExplanation(), started);
        }

        // Stage 3: Drools execute against the resolved bundle.
        RuleEnginePort.ExecuteInput input = new RuleEnginePort.ExecuteInput(
                bundle.getBundleHash(),
                buildDroolsContext(request),
                new RuleEnginePort.ExecuteOptions("NONE", 30000, 1000));

        ExecuteResponse executed;
        try {
            executed = ruleEnginePort.execute(input);
        } catch (Exception e) {
            log.error("Drools execute failed for bundleHash={}", bundle.getBundleHash(), e);
            return deny(result, "EXECUTE", "EXECUTION_ERROR", e.getMessage(), started);
        }

        result.setStage("EXECUTE");
        result.setDecision(executed.getDecision());
        result.setOk(executed.getOk());
        result.setReasonCodes(executed.getReasonCodes() != null ? executed.getReasonCodes() : Collections.emptyList());
        result.setLatencyMs(System.currentTimeMillis() - started);
        return result;
    }

    private SubjectResult deny(SubjectResult result, String stage, String reasonCode,
                               String explanation, long started) {
        result.setStage(stage);
        result.setDecision("DENY");
        result.setOk(false);
        result.setReasonCodes(List.of(reasonCode));
        result.setExplanation(explanation);
        result.setLatencyMs(System.currentTimeMillis() - started);
        return result;
    }

    private FastCheckRequest toFastCheckRequest(BatchEvaluateRequest request,
                                                BatchEvaluateRequest.Subject subject) {
        FastCheckRequest fc = new FastCheckRequest();
        fc.setSubjectType(subject.getSubjectType());
        fc.setCampaignId(subject.getSubjectKey());
        if (request.getCustomer() != null) {
            fc.setCustomerId(request.getCustomer().id());
            fc.setCustomerSegments(request.getCustomer().segments());
            fc.setCustomerRegion(request.getCustomer().region());
        }
        if (request.getOrder() != null) {
            BigDecimal total = request.getOrder().total();
            fc.setOrderTotal(total != null ? total.longValue() : 0L);
            fc.setCurrency(request.getOrder().currency());
            fc.setItemCount(request.getOrder().items() != null ? request.getOrder().items().size() : 0);
        }
        fc.setEvaluationTime(request.getEvaluationTime());
        fc.setTimezone(request.getTimezone());
        return fc;
    }

    private Map<String, Object> buildDroolsContext(BatchEvaluateRequest request) {
        Map<String, Object> context = new HashMap<>();
        context.put("customer", request.getCustomer());
        context.put("order", request.getOrder());
        context.put("candidate", request.getCandidate());
        context.put("executionContext", request.getExecutionContext());
        return context;
    }
}
