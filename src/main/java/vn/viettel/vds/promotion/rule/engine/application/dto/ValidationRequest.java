package vn.viettel.vds.promotion.rule.engine.application.dto;

import vn.viettel.vds.promotion.rule.engine.domain.model.*;

import java.util.List;
import java.util.Map;

// Sonar rules S100/S107/S1172 are false positives on Java records (older sonar-java plugins
// analyze record components/canonical constructor as regular methods with too many/unused params).
@SuppressWarnings({"java:S100", "java:S107", "java:S1172"})
public record ValidationRequest( // NOSONAR
        Customer customer,
        Order order,
        Redemption redemption,
        LimitsCtx limits,
        EnvCtx env,
        List<Candidate> candidates,
        String channel,
        Geo geo,
        Map<String, Object> metadata
) {
    public ValidationRequest(Customer customer, Order order, List<Candidate> candidates) {
        this(customer, order, null, null, null, candidates, null, null, null);
    }
}