package vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

// Sonar rules S100/S1172/S1186 are false positives on Java records (older sonar-java plugins
// analyze record components/canonical constructor as regular methods).
@Schema(description = "Customer data for rule execution")
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings({"java:S100", "java:S1172", "java:S1186"})
public record CustomerDto( // NOSONAR
        @Schema(description = "Customer identifier", example = "cust123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Customer ID is required")
        @JsonProperty("id")
        String id,

        @Schema(description = "Customer segments", example = "[\"VIP\", \"GOLD\"]")
        @JsonProperty("segments")
        List<String> segments,

        @Schema(description = "Customer region", example = "HCM")
        @JsonProperty("region")
        String region,

        @Schema(description = "Customer tier", example = "3")
        @JsonProperty("tier")
        Integer tier,

        @Schema(description = "Customer metadata")
        @JsonProperty("metadata")
        Map<String, Object> metadata
) {
    // Empty body intentional — Java record canonical constructor is implicit.
}