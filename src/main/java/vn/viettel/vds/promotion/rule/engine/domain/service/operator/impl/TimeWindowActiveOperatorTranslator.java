package vn.viettel.vds.promotion.rule.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.rule.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;

@Component
public class TimeWindowActiveOperatorTranslator implements OperatorTranslator {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TimeWindowActiveOperatorTranslator.class);

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        logger.debug("Translating time.window.active operator for node: {}, params: {}", nodeId, params);

        validateRequiredParameters(params);

        String startTime = extractParameter(params, "startTime").toString();
        String endTime = extractParameter(params, "endTime").toString();
        String timezone = extractTimezone(params);
        boolean spansMidnight = extractSpansMidnight(params);
        String daysOfWeekCsv = buildDaysOfWeekCsv(params.get("daysOfWeek"));

        String result = buildTimeWindowCondition(startTime, endTime, timezone, spansMidnight, daysOfWeekCsv);
        logger.debug("Generated DRL condition: {}", result);
        return result;
    }

    private void validateRequiredParameters(Map<String, Object> params) {
        Object startTimeParam = params.get("startTime");
        Object endTimeParam = params.get("endTime");

        if (startTimeParam == null || endTimeParam == null) {
            throw new IllegalArgumentException("Missing required parameters 'startTime' and 'endTime' for time.window.active operator");
        }
    }

    private Object extractParameter(Map<String, Object> params, String key) {
        return params.get(key);
    }

    private String extractTimezone(Map<String, Object> params) {
        Object timezoneParam = params.get("timezone");
        return timezoneParam != null ? timezoneParam.toString() : "UTC";
    }

    private boolean extractSpansMidnight(Map<String, Object> params) {
        Object spansMidnightParam = params.get("spansMidnight");
        return spansMidnightParam != null && Boolean.parseBoolean(spansMidnightParam.toString());
    }

    private String buildDaysOfWeekCsv(Object daysOfWeekParam) {
        if (daysOfWeekParam == null) {
            return "";
        }

        logger.debug("daysOfWeekParam type: {}, value: {}", daysOfWeekParam.getClass().getName(), daysOfWeekParam);

        if (!(daysOfWeekParam instanceof List)) {
            logger.warn("daysOfWeek is not a List, it's: {}", daysOfWeekParam.getClass().getName());
            return "";
        }

        return convertListToCsv((List<?>) daysOfWeekParam);
    }

    private String convertListToCsv(List<?> daysList) {
        StringBuilder csvBuilder = new StringBuilder();
        for (int i = 0; i < daysList.size(); i++) {
            if (i > 0) {
                csvBuilder.append(",");
            }
            Object dayObj = daysList.get(i);
            String dayStr = dayObj != null ? dayObj.toString() : "";
            csvBuilder.append(dayStr.toUpperCase());
        }
        return csvBuilder.toString();
    }

    private String buildTimeWindowCondition(String startTime, String endTime, String timezone,
                                            boolean spansMidnight, String daysOfWeekCsv) {
        return String.format("eval(checkTimeWindow(\"%s\", \"%s\", \"%s\", %s, \"%s\"))",
                startTime, endTime, timezone, spansMidnight, daysOfWeekCsv);
    }

    @Override
    public String getOperatorName() {
        return "time.window.active";
    }

    @Override
    public Integer getVersion() {
        return 1;
    }

    @Override
    public boolean supports(String operatorName, Integer version) {
        return "time.window.active".equals(operatorName) &&
                (version == null || version.equals(getVersion()));
    }
}