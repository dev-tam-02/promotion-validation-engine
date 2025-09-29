package vn.viettel.vds.promotion.validation.engine.application.port.in;

import vn.viettel.vds.promotion.validation.engine.application.dto.HealthResponse;

public interface HealthCheckUseCase {

    HealthResponse getHealth();
}