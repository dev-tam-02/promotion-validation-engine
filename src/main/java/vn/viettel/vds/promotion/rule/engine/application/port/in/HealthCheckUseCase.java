package vn.viettel.vds.promotion.rule.engine.application.port.in;

import vn.viettel.vds.promotion.rule.engine.application.dto.HealthResponse;

public interface HealthCheckUseCase {

    HealthResponse getHealth();
}