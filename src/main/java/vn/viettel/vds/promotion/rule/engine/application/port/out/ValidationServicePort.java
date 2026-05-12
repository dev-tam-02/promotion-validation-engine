package vn.viettel.vds.promotion.rule.engine.application.port.out;

import java.time.Instant;
import java.util.List;

public interface ValidationServicePort {

    List<RuleBindingDto> fetchActiveBindings(int page, int size);

    RuleBindingDto fetchBindingByObjectAndRule(String objectType, String objectId, String ruleId);

    // Sonar rules S100/S107/S1186 are false positives on Java records (older sonar-java plugins
    // analyze record components/canonical constructor as regular methods).
    @SuppressWarnings({"java:S100", "java:S107", "java:S1186"})
    record RuleBindingDto( // NOSONAR
            String id,
            String ruleId,
            String objectType,
            String objectId,
            Integer priority,
            Boolean active,
            Instant validFrom,
            Instant validTo,
            String timezone,
            String rrule,
            List<TimeWindowDto> timeWindows,
            List<String> excludedDates,
            Boolean includedAll,
            List<String> includedProducts,
            List<String> excludedProducts,
            List<String> includedCategories,
            List<String> excludedCategories,
            List<String> includedBrands,
            List<String> excludedBrands,
            Integer trafficPercent,
            String stickyKeyStrategy,
            String bundleHash,
            Long version
    ) {
        // Empty body intentional — Java record canonical constructor is implicit.
    }

    @SuppressWarnings({"java:S100", "java:S1186"})
    record TimeWindowDto(String start, String end) { // NOSONAR
        // Empty body intentional — Java record canonical constructor is implicit.
    }
}
