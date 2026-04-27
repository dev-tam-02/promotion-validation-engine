package vn.viettel.vds.promotion.rule.engine.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private boolean matched;
    private String message;
    private String candidateId;

    // Additional fields for rule execution
    private Boolean ok;
    private String decision;
    private List<String> reasonCodes;

    // Quota policies emitted by counter rules; enforced post-eval by QuotaCounterService
    private final List<QuotaPolicy> policies = new ArrayList<>();

    public ValidationResult() {
    }

    public ValidationResult(boolean matched, String message) {
        this.matched = matched;
        this.message = message;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public List<String> getReasonCodes() {
        return reasonCodes;
    }

    public void setReasonCodes(List<String> reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    public void addPolicy(QuotaPolicy policy) {
        policies.add(policy);
    }

    public List<QuotaPolicy> getPolicies() {
        return Collections.unmodifiableList(policies);
    }

    public boolean hasPolicies() {
        return !policies.isEmpty();
    }
}