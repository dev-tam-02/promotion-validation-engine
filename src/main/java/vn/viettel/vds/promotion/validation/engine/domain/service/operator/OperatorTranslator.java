package vn.viettel.vds.promotion.validation.engine.domain.service.operator;

import java.util.Map;

public interface OperatorTranslator {

    String translate(String nodeId, Map<String, Object> params, String reasonCode);

    String getOperatorName();

    Integer getVersion();

    boolean supports(String operatorName, Integer version);
}