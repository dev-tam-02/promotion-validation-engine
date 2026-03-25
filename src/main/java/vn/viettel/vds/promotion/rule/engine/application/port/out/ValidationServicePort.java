package vn.viettel.vds.promotion.rule.engine.application.port.out;

import java.time.Instant;
import java.util.List;

public interface ValidationServicePort {

    List<RuleBindingDto> fetchActiveBindings(int page, int size);

    RuleBindingDto fetchBindingByObjectAndRule(String objectType, String objectId, String ruleId);

    record RuleBindingDto(
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
    ) {}

    record TimeWindowDto(String start, String end) {}
}
