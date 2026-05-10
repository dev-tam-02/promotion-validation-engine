package vn.viettel.vds.promotion.rule.engine.application.dto;

import vn.viettel.vds.promotion.rule.engine.domain.model.Decision;

import java.util.List;

// Sonar rules S100/S1172/S1186 are false positives on Java records (older sonar-java plugins
// analyze record components/canonical constructor as regular methods).
@SuppressWarnings({"java:S100", "java:S1172", "java:S1186"})
public record ValidationResponse( // NOSONAR
        List<Decision> decisions
) {
    // Empty body intentional — Java record canonical constructor is implicit.
}