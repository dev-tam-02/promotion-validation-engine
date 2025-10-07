package vn.viettel.vds.promotion.validation.engine.application.dto;

import vn.viettel.vds.promotion.validation.engine.domain.model.*;

import java.util.List;
import java.util.Map;

public record ValidationRequest(
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