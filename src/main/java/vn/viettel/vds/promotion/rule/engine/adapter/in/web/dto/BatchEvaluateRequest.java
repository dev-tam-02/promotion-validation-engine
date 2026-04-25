package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Single-hop batch-evaluate request used by pp-redemption. Carries a
 * shared execution context plus a list of (subjectType, subjectKey)
 * pairs to evaluate. The rule engine resolves each subject's bundle,
 * runs fast-check, then Drools execution, and returns a per-subject
 * decision so the redemption flow never needs a separate bundle lookup
 * round-trip.
 */
@Schema(description = "Batch evaluate request — one shared context, many subjects")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchEvaluateRequest {

    @NotNull
    @Valid
    @JsonProperty("customer")
    private CustomerDto customer;

    @NotNull
    @Valid
    @JsonProperty("order")
    private OrderDto order;

    @Valid
    @JsonProperty("candidate")
    private CandidateDto candidate;

    @Valid
    @JsonProperty("executionContext")
    private ExecutionContextDto executionContext;

    @Schema(description = "Evaluation timestamp used for fast-check time constraints")
    @JsonProperty("evaluationTime")
    private OffsetDateTime evaluationTime;

    @Schema(description = "Timezone for evaluation", example = "Asia/Ho_Chi_Minh")
    @JsonProperty("timezone")
    private String timezone;

    @NotEmpty(message = "At least one subject is required")
    @Valid
    @JsonProperty("subjects")
    private List<Subject> subjects;

    @Schema(description = "Subject reference — canonical (type, key) pair")
    public static class Subject {
        @NotBlank
        @JsonProperty("subjectType")
        private String subjectType;

        @NotBlank
        @JsonProperty("subjectKey")
        private String subjectKey;

        public Subject() {}

        public Subject(String subjectType, String subjectKey) {
            this.subjectType = subjectType;
            this.subjectKey = subjectKey;
        }

        public String getSubjectType() { return subjectType; }
        public void setSubjectType(String subjectType) { this.subjectType = subjectType; }

        public String getSubjectKey() { return subjectKey; }
        public void setSubjectKey(String subjectKey) { this.subjectKey = subjectKey; }
    }

    public CustomerDto getCustomer() { return customer; }
    public void setCustomer(CustomerDto customer) { this.customer = customer; }

    public OrderDto getOrder() { return order; }
    public void setOrder(OrderDto order) { this.order = order; }

    public CandidateDto getCandidate() { return candidate; }
    public void setCandidate(CandidateDto candidate) { this.candidate = candidate; }

    public ExecutionContextDto getExecutionContext() { return executionContext; }
    public void setExecutionContext(ExecutionContextDto executionContext) { this.executionContext = executionContext; }

    public OffsetDateTime getEvaluationTime() { return evaluationTime; }
    public void setEvaluationTime(OffsetDateTime evaluationTime) { this.evaluationTime = evaluationTime; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public List<Subject> getSubjects() { return subjects; }
    public void setSubjects(List<Subject> subjects) { this.subjects = subjects; }
}
