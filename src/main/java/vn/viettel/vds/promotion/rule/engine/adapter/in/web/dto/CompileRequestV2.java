package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CompileRequestV2 {

    @NotBlank
    @JsonProperty("ruleId")
    private String ruleId;

    @NotNull
    @JsonProperty("version")
    private Integer version;

    @JsonProperty("logic")
    private String logic;

    @Valid
    @JsonProperty("nodes")
    private List<RuleNodeDto> nodes;

    @JsonProperty("timeLinks")
    private List<TimeLinkDto> timeLinks;

    // Constructors
    public CompileRequestV2() {
    }

    public CompileRequestV2(String ruleId, Integer version, String logic, List<RuleNodeDto> nodes) {
        this.ruleId = ruleId;
        this.version = version;
        this.logic = logic;
        this.nodes = nodes;
    }

    // Getters and setters
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        this.logic = logic;
    }

    public List<RuleNodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<RuleNodeDto> nodes) {
        this.nodes = nodes;
    }

    public List<TimeLinkDto> getTimeLinks() {
        return timeLinks;
    }

    public void setTimeLinks(List<TimeLinkDto> timeLinks) {
        this.timeLinks = timeLinks;
    }

    public static class TimeLinkDto {
        @JsonProperty("id")
        private String id;

        @JsonProperty("data")
        private Object data;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
