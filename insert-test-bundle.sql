-- SQL Script to insert test bundle for validation-engine
-- This allows the engine to load the DRL content from database

-- Delete existing bundle with the same hash (if any)
DELETE FROM bundles WHERE id = 'sha256:bbfe8e568dcfe134b80813797937214bf24500729ac330360a23a45970666a1f';

-- Insert the bundle with DRL content
INSERT INTO bundles (
    id,
    tenant_id,
    rule_id,
    rule_version,
    operators_fingerprint,
    engine_type,
    compiler_id,
    drools_version,
    limit_per_customer,
    limit_per_day,
    artifact_store,
    artifact_key,
    artifact_size,
    created_at,
    validation_rule_version_id,
    snapshot_hash,
    drl_content
) VALUES (
    'sha256:bbfe8e568dcfe134b80813797937214bf24500729ac330360a23a45970666a1f',
    'defaulttenant',
    'promotion_validation_rule',
    1,
    NULL,
    'drools',
    'drools-compiler-v1',
    'drools-10.1.0',
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NOW(),
    NULL,
    NULL,
    'package tenant.defaulttenant;

import vn.viettel.vds.promotion.validation.engine.domain.model.Customer;
import vn.viettel.vds.promotion.validation.engine.domain.model.Order;
import vn.viettel.vds.promotion.validation.engine.domain.model.OrderItem;
import vn.viettel.vds.promotion.validation.engine.domain.model.Candidate;
import vn.viettel.vds.promotion.validation.engine.domain.model.ValidationResult;
import java.util.List;
import java.util.ArrayList;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.time.LocalTime;

global ValidationResult result;
global List<String> reasonCodes;
global Object usageService;

function boolean checkTimeWindow(String startTime, String endTime, String timezone, boolean spansMidnight, String daysOfWeekCsv) {
    ZonedDateTime zdt = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.of(timezone));
    DayOfWeek currentDay = zdt.getDayOfWeek();
    LocalTime currentTime = zdt.toLocalTime();
    LocalTime start = LocalTime.parse(startTime);
    LocalTime end = LocalTime.parse(endTime);

    // Check day of week if specified
    if (daysOfWeekCsv != null && !daysOfWeekCsv.isEmpty()) {
        boolean dayMatches = false;
        String[] daysOfWeek = daysOfWeekCsv.split(",");
        for (String day : daysOfWeek) {
            if (currentDay.name().equals(day.trim())) {
                dayMatches = true;
                break;
            }
        }
        if (!dayMatches) return false;
    }

    // Check time range
    if (spansMidnight) {
        return currentTime.isAfter(start) || currentTime.equals(start) || currentTime.isBefore(end) || currentTime.equals(end);
    } else {
        return (currentTime.isAfter(start) || currentTime.equals(start)) && (currentTime.isBefore(end) || currentTime.equals(end));
    }
}

rule "promotion_validation_rule"
    when
  $order: Order(total >= 500000)
  $customer: Customer(segments contains "VIP")
    then
        result.setDecision("ALLOW");
        result.setOk(true);
end

rule "failure_tracking_n3"
    salience -10
    when
        not ValidationResult(decision == "ALLOW")
        not (
            $order: Order(total >= 500000)
        )
    then
        reasonCodes.add("ORDER_TOTAL_MIN");
end

rule "failure_tracking_n4"
    salience -10
    when
        not ValidationResult(decision == "ALLOW")
        not (
            $customer: Customer(segments contains "VIP")
        )
    then
        reasonCodes.add("AUDIENCE_SEGMENT");
end

rule "promotion_validation_failure"
    salience -100
    when
        not ValidationResult(decision == "ALLOW")
    then
        result.setDecision("DENY");
        result.setOk(false);
        result.setReasonCodes(reasonCodes);
end'
);

-- Verify the insertion
SELECT
    id,
    tenant_id,
    rule_id,
    rule_version,
    drools_version,
    created_at,
    LENGTH(drl_content) as drl_content_length
FROM bundles
WHERE id = 'sha256:bbfe8e568dcfe134b80813797937214bf24500729ac330360a23a45970666a1f';
