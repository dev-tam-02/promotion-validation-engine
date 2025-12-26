package vn.viettel.vds.promotion.rule.engine.application.port.in;

import vn.viettel.vds.promotion.rule.engine.application.dto.ValidationRequest;
import vn.viettel.vds.promotion.rule.engine.application.dto.ValidationResponse;

public interface ValidatePromotionUseCase {

    ValidationResponse validatePromotions(ValidationRequest request);
}