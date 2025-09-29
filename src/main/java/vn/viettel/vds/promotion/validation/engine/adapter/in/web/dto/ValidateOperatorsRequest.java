package vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ValidateOperatorsRequest {

    @JsonProperty("operators")
    @NotNull(message = "Operators list is required")
    @NotEmpty(message = "Operators list cannot be empty")
    @Valid
    private List<OperatorValidationItem> operators;

    public ValidateOperatorsRequest() {
    }

    public ValidateOperatorsRequest(List<OperatorValidationItem> operators) {
        this.operators = operators;
    }

    public List<OperatorValidationItem> getOperators() {
        return operators;
    }

    public void setOperators(List<OperatorValidationItem> operators) {
        this.operators = operators;
    }

    public static class OperatorValidationItem {
        @JsonProperty("operatorName")
        @NotNull(message = "Operator name is required")
        private String operatorName;

        @JsonProperty("version")
        private Integer version;

        public OperatorValidationItem() {
        }

        public OperatorValidationItem(String operatorName, Integer version) {
            this.operatorName = operatorName;
            this.version = version;
        }

        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(String operatorName) {
            this.operatorName = operatorName;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }
}