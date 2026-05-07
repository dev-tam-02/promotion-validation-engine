package vn.viettel.vds.promotion.rule.engine.application.port.in;

import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.BatchEvaluateRequest;
import vn.viettel.vds.promotion.rule.engine.adapter.in.web.dto.BatchEvaluateResponse;

/**
 * Single-hop evaluation pipeline used by pp-redemption.
 * <p>
 * For each subject in the request, the rule engine resolves the bundle,
 * runs fast-check, and if fast-check allows, invokes full Drools
 * execution. The returned response carries one decision per subject so
 * the caller makes only one network round-trip.
 */
public interface BatchEvaluateUseCase {

    BatchEvaluateResponse evaluate(BatchEvaluateRequest request);
}
