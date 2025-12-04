-- =============================================================================
-- Validation Engine - Sample/Initial Data for MariaDB
-- Generated from Liquibase changelog files
-- =============================================================================

-- Lưu ý: Các file Liquibase gốc không chứa dữ liệu INSERT.
-- File này được tạo sẵn để thêm dữ liệu mẫu nếu cần.

-- -----------------------------------------------------------------------------
-- Sample data cho engine_configs (nếu cần)
-- -----------------------------------------------------------------------------
-- INSERT INTO engine_configs (id, tenant_id, timeout_ms, max_rules_fired, max_facts, max_nodes, max_depth, created_at)
-- VALUES ('config-default', 'DEFAULT_TENANT', 5000, 1000, 10000, 5000, 100, NOW());

-- -----------------------------------------------------------------------------
-- Sample data cho validation_rules (nếu cần)
-- -----------------------------------------------------------------------------
-- INSERT INTO validation_rules (name, version, drl_text, enabled, created_at)
-- VALUES ('sample-rule', '1.0.0', 'package rules; rule "Sample" when then end', TRUE, NOW());

-- -----------------------------------------------------------------------------
-- Sample data cho time_policies (nếu cần)
-- -----------------------------------------------------------------------------
-- INSERT INTO time_policies (id, tenant_id, name, description, active, created_at)
-- VALUES ('policy-business-hours', 'DEFAULT_TENANT', 'Business Hours Policy', 'Chỉ cho phép trong giờ làm việc', TRUE, NOW());

-- INSERT INTO time_windows (policy_id, days_of_week, start_time, end_time, spans_midnight)
-- VALUES ('policy-business-hours', 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', '08:00:00', '17:00:00', FALSE);
