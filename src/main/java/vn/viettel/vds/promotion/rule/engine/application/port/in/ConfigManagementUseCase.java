package vn.viettel.vds.promotion.rule.engine.application.port.in;

import vn.viettel.vds.promotion.rule.engine.application.dto.EngineConfigResponse;
import vn.viettel.vds.promotion.rule.engine.application.dto.EngineConfigUpdateRequest;

public interface ConfigManagementUseCase {

    EngineConfigResponse getConfig(String tenantId);

    EngineConfigResponse updateConfig(String tenantId, EngineConfigUpdateRequest request);
}