package vn.viettel.vds.promotion.validation.engine.application.port.in;

import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.FastCheckRequest;
import vn.viettel.vds.promotion.validation.engine.adapter.in.web.dto.FastCheckResponse;

public interface FastCheckUseCase {

    FastCheckResponse performFastCheck(FastCheckRequest request);
}