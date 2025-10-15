package vn.viettel.vds.promotion.validation.engine.domain.service.operator.impl;

import org.springframework.stereotype.Component;
import vn.viettel.vds.promotion.validation.engine.domain.service.operator.OperatorTranslator;

import java.util.List;
import java.util.Map;

@Component
public class TimeWindowActiveOperatorTranslator implements OperatorTranslator {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TimeWindowActiveOperatorTranslator.class);

    @Override
    public String translate(String nodeId, Map<String, Object> params, String reasonCode) {
        logger.debug("Translating time.window.active operator for node: {}, params: {}", nodeId, params);

        // Extract time window parameters
        Object daysOfWeekParam = params.get("daysOfWeek");
        Object startTimeParam = params.get("startTime");
        Object endTimeParam = params.get("endTime");
        Object timezoneParam = params.get("timezone");
        Object spansMidnightParam = params.get("spansMidnight");

        logger.debug("Extracted params - startTime: {}, endTime: {}, timezone: {}, daysOfWeek: {} (type: {}), spansMidnight: {}",
            startTimeParam, endTimeParam, timezoneParam, daysOfWeekParam,
            daysOfWeekParam != null ? daysOfWeekParam.getClass().getName() : "null",
            spansMidnightParam);

        // Validate required parameters
        if (startTimeParam == null || endTimeParam == null) {
            throw new IllegalArgumentException("Missing required parameters 'startTime' and 'endTime' for time.window.active operator");
        }

        String startTime = startTimeParam.toString();
        String endTime = endTimeParam.toString();
        String timezone = timezoneParam != null ? timezoneParam.toString() : "UTC";
        boolean spansMidnight = spansMidnightParam != null && Boolean.parseBoolean(spansMidnightParam.toString());

        // Build days of week CSV string for function call
        String daysOfWeekCsv = "";
        if (daysOfWeekParam != null) {
            logger.debug("daysOfWeekParam type: {}, value: {}", daysOfWeekParam.getClass().getName(), daysOfWeekParam);

            if (daysOfWeekParam instanceof List) {
                @SuppressWarnings("unchecked")
                List<?> daysList = (List<?>) daysOfWeekParam;
                StringBuilder csvBuilder = new StringBuilder();
                for (int i = 0; i < daysList.size(); i++) {
                    Object dayObj = daysList.get(i);
                    if (i > 0) csvBuilder.append(",");
                    String dayStr = dayObj != null ? dayObj.toString() : "";
                    csvBuilder.append(dayStr.toUpperCase());
                }
                daysOfWeekCsv = csvBuilder.toString();
            } else {
                logger.warn("daysOfWeek is not a List, it's: {}", daysOfWeekParam.getClass().getName());
            }
        }

        // Generate function call with CSV string for days
        StringBuilder sb = new StringBuilder();
        sb.append("eval(checkTimeWindow(\"").append(startTime).append("\", \"")
          .append(endTime).append("\", \"").append(timezone).append("\", ")
          .append(spansMidnight).append(", \"")
          .append(daysOfWeekCsv).append("\"))");

        String result = sb.toString();
        logger.debug("Generated DRL condition: {}", result);
        return result;
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