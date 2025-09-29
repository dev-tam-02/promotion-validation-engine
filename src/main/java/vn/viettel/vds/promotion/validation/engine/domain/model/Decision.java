package vn.viettel.vds.promotion.validation.engine.domain.model;

import java.util.List;
import java.util.Map;

public class Decision {
    private Candidate candidate;
    private boolean valid;
    private List<ReasonCode> reasons;
    private Map<String, Integer> limitsRemaining;
    private String decisionToken;

    public Decision() {
    }

    public Decision(Candidate candidate, boolean valid) {
        this.candidate = candidate;
        this.valid = valid;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ReasonCode> getReasons() {
        return reasons;
    }

    public void setReasons(List<ReasonCode> reasons) {
        this.reasons = reasons;
    }

    public Map<String, Integer> getLimitsRemaining() {
        return limitsRemaining;
    }

    public void setLimitsRemaining(Map<String, Integer> limitsRemaining) {
        this.limitsRemaining = limitsRemaining;
    }

    public String getDecisionToken() {
        return decisionToken;
    }

    public void setDecisionToken(String decisionToken) {
        this.decisionToken = decisionToken;
    }
}