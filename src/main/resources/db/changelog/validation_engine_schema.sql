-- =============================================================================
-- Validation Engine - Database Schema for MariaDB
-- Generated from Liquibase changelog files
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Table: bundles
-- Description: Lưu trữ các bundle đã được compile từ Drools rules
-- -----------------------------------------------------------------------------
CREATE TABLE bundles
(
    id                         VARCHAR(300) NOT NULL COMMENT 'ID duy nhất của bundle (hash)',
    rule_id                    VARCHAR(100) NOT NULL COMMENT 'ID của rule được compile',
    rule_version               INT          NOT NULL COMMENT 'Version của rule',
    operators_fingerprint      VARCHAR(255) COMMENT 'Fingerprint của các operators trong rule',
    engine_type                VARCHAR(50) COMMENT 'Loại engine sử dụng (DROOLS, etc.)',
    compiler_id                VARCHAR(100) COMMENT 'ID của compiler đã tạo bundle',
    drools_version             VARCHAR(20) COMMENT 'Version của Drools được sử dụng',
    limit_per_customer         INT COMMENT 'Giới hạn số lần sử dụng mỗi customer',
    limit_per_day              INT COMMENT 'Giới hạn số lần sử dụng mỗi ngày',
    artifact_store             VARCHAR(50) COMMENT 'Nơi lưu trữ artifact (S3, LOCAL, etc.)',
    artifact_key               VARCHAR(255) COMMENT 'Key để truy xuất artifact',
    artifact_size              BIGINT COMMENT 'Kích thước artifact (bytes)',
    created_at                 TIMESTAMP    NOT NULL COMMENT 'Thời điểm tạo bundle',
    validation_rule_version_id VARCHAR(100) COMMENT 'ID version của validation rule',
    snapshot_hash              VARCHAR(255) COMMENT 'Hash của snapshot',
    drl_content                TEXT COMMENT 'Nội dung DRL gốc',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho bundles
CREATE UNIQUE INDEX idx_bundle_hash ON bundles (id);
CREATE INDEX idx_rule_version ON bundles (rule_id, rule_version);

-- -----------------------------------------------------------------------------
-- Table: bundle_time_links
-- Description: Liên kết bundle với time policy
-- -----------------------------------------------------------------------------
CREATE TABLE bundle_time_links
(
    id        BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID tự động tăng',
    policy_id VARCHAR(100) COMMENT 'ID của time policy',
    mode      VARCHAR(50) COMMENT 'Mode áp dụng (ALLOW, DENY, etc.)',
    bundle_id VARCHAR(300) COMMENT 'ID của bundle liên kết',
    PRIMARY KEY (id),
    CONSTRAINT fk_bundle_time_links_bundle
        FOREIGN KEY (bundle_id) REFERENCES bundles (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: bundle_subject_index
-- Description: Index để tra cứu bundle theo subject (campaign, promotion, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE bundle_subject_index
(
    id                 VARCHAR(200) NOT NULL COMMENT 'ID duy nhất của index entry',
    subject_type       VARCHAR(50)  NOT NULL COMMENT 'Loại subject (CAMPAIGN, PROMOTION, etc.)',
    subject_key        VARCHAR(100) NOT NULL COMMENT 'Key của subject',
    rule_id            VARCHAR(100) NOT NULL COMMENT 'ID của rule',
    rule_version       INT          NOT NULL COMMENT 'Version của rule',
    assignment_version INT COMMENT 'Version của assignment',
    bundle_hash        VARCHAR(300) COMMENT 'Hash của bundle được gán',
    updated_at         TIMESTAMP    NOT NULL COMMENT 'Thời điểm cập nhật cuối',
    PRIMARY KEY (id),
    CONSTRAINT uk_subject UNIQUE (subject_type, subject_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho bundle_subject_index
CREATE INDEX idx_subject ON bundle_subject_index (subject_type, subject_key);
CREATE INDEX idx_rule_ver ON bundle_subject_index (rule_id, rule_version);

-- -----------------------------------------------------------------------------
-- Table: bundle_warmups
-- Description: Theo dõi trạng thái warmup của bundle trong memory
-- -----------------------------------------------------------------------------
CREATE TABLE bundle_warmups
(
    id          VARCHAR(200) NOT NULL COMMENT 'ID duy nhất của warmup entry',
    bundle_hash VARCHAR(300) NOT NULL COMMENT 'Hash của bundle cần warmup',
    state       VARCHAR(20)  NOT NULL COMMENT 'Trạng thái warmup (PENDING, WARMING, READY, FAILED)',
    attempts    INT COMMENT 'Số lần thử warmup',
    created_at  TIMESTAMP    NOT NULL COMMENT 'Thời điểm tạo',
    updated_at  TIMESTAMP COMMENT 'Thời điểm cập nhật cuối',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho bundle_warmups
CREATE INDEX idx_state ON bundle_warmups (state, created_at);

-- -----------------------------------------------------------------------------
-- Table: compile_jobs
-- Description: Quản lý các job compile rule thành bundle
-- -----------------------------------------------------------------------------
CREATE TABLE compile_jobs
(
    id                    VARCHAR(200) NOT NULL COMMENT 'ID duy nhất của compile job',
    rule_id               VARCHAR(100) NOT NULL COMMENT 'ID của rule cần compile',
    target_version        INT          NOT NULL COMMENT 'Version đích cần compile',
    status                VARCHAR(20)  NOT NULL COMMENT 'Trạng thái job (PENDING, RUNNING, COMPLETED, FAILED)',
    requested_by          VARCHAR(100) COMMENT 'User/System yêu cầu compile',
    requested_at          TIMESTAMP    NOT NULL COMMENT 'Thời điểm yêu cầu',
    completed_at          TIMESTAMP COMMENT 'Thời điểm hoàn thành',
    operators_fingerprint VARCHAR(255) COMMENT 'Fingerprint của operators',
    compiler_id           VARCHAR(100) COMMENT 'ID của compiler xử lý job',
    bundle_hash           VARCHAR(300) COMMENT 'Hash của bundle kết quả',
    PRIMARY KEY (id),
    CONSTRAINT uk_rule_target UNIQUE (rule_id, target_version)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho compile_jobs
CREATE INDEX idx_status_time ON compile_jobs (status, requested_at);

-- -----------------------------------------------------------------------------
-- Table: compile_job_logs
-- Description: Log chi tiết của quá trình compile
-- -----------------------------------------------------------------------------
CREATE TABLE compile_job_logs
(
    id             BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID tự động tăng',
    level          VARCHAR(20) COMMENT 'Log level (INFO, WARN, ERROR, DEBUG)',
    msg            TEXT COMMENT 'Nội dung log message',
    timestamp      TIMESTAMP COMMENT 'Thời điểm ghi log',
    compile_job_id VARCHAR(200) COMMENT 'ID của compile job liên quan',
    PRIMARY KEY (id),
    CONSTRAINT fk_compile_job_logs_job
        FOREIGN KEY (compile_job_id) REFERENCES compile_jobs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: compile_job_errors
-- Description: Lưu trữ lỗi của compile job
-- -----------------------------------------------------------------------------
CREATE TABLE compile_job_errors
(
    compile_job_id VARCHAR(200) NOT NULL COMMENT 'ID của compile job',
    error_message  VARCHAR(1000) COMMENT 'Nội dung lỗi',
    CONSTRAINT fk_compile_job_errors_job
        FOREIGN KEY (compile_job_id) REFERENCES compile_jobs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: engine_configs
-- Description: Cấu hình cho validation engine
-- -----------------------------------------------------------------------------
CREATE TABLE engine_configs
(
    id              VARCHAR(100) NOT NULL COMMENT 'ID duy nhất của config',
    timeout_ms      INT COMMENT 'Timeout cho validation (milliseconds)',
    max_rules_fired INT COMMENT 'Số rule tối đa được fire trong 1 session',
    max_facts       INT COMMENT 'Số facts tối đa trong working memory',
    max_nodes       INT COMMENT 'Số nodes tối đa trong RETE network',
    max_depth       INT COMMENT 'Độ sâu tối đa của rule chain',
    created_at      TIMESTAMP    NOT NULL COMMENT 'Thời điểm tạo',
    updated_at      TIMESTAMP COMMENT 'Thời điểm cập nhật cuối',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: engine_config_explain_sampling
-- Description: Cấu hình sampling cho explain mode
-- -----------------------------------------------------------------------------
CREATE TABLE engine_config_explain_sampling
(
    engine_config_id VARCHAR(100) NOT NULL COMMENT 'ID của engine config',
    sampling_key     VARCHAR(100) NOT NULL COMMENT 'Key của sampling config',
    sampling_value   DOUBLE COMMENT 'Giá trị sampling (0.0 - 1.0)',
    CONSTRAINT fk_explain_sampling_config
        FOREIGN KEY (engine_config_id) REFERENCES engine_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: outbox_events
-- Description: Outbox pattern - lưu events cần dispatch
-- -----------------------------------------------------------------------------
CREATE TABLE outbox_events
(
    id            VARCHAR(100) NOT NULL COMMENT 'ID duy nhất của event',
    type          VARCHAR(30)  NOT NULL COMMENT 'Loại event (BUNDLE_COMPILED, RULE_UPDATED, etc.)',
    status        VARCHAR(20)  NOT NULL COMMENT 'Trạng thái (PENDING, DISPATCHED, FAILED)',
    attempts      INT COMMENT 'Số lần thử dispatch',
    created_at    TIMESTAMP    NOT NULL COMMENT 'Thời điểm tạo event',
    last_tried_at TIMESTAMP COMMENT 'Thời điểm thử dispatch cuối',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho outbox_events
CREATE INDEX idx_dispatch_queue ON outbox_events (status, created_at);

-- -----------------------------------------------------------------------------
-- Table: outbox_event_payload
-- Description: Payload của outbox event (key-value)
-- -----------------------------------------------------------------------------
CREATE TABLE outbox_event_payload
(
    outbox_event_id VARCHAR(100) NOT NULL COMMENT 'ID của outbox event',
    payload_key     VARCHAR(100) NOT NULL COMMENT 'Key của payload entry',
    payload_value   TEXT COMMENT 'Value của payload entry',
    CONSTRAINT fk_outbox_payload_event
        FOREIGN KEY (outbox_event_id) REFERENCES outbox_events (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: validation_rules
-- Description: Lưu trữ các validation rules định nghĩa bằng DRL
-- -----------------------------------------------------------------------------
CREATE TABLE validation_rules
(
    id         BIGINT                                                        NOT NULL AUTO_INCREMENT COMMENT 'ID tự động tăng',
    name       VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Tên của rule (unique)',
    version    VARCHAR(50) COMMENT 'Version string của rule',
    drl_text   TEXT COMMENT 'Nội dung DRL của rule',
    enabled    BOOLEAN                                                       NOT NULL DEFAULT TRUE COMMENT 'Trạng thái enable/disable',
    created_at TIMESTAMP                                                     NOT NULL COMMENT 'Thời điểm tạo',
    updated_at TIMESTAMP COMMENT 'Thời điểm cập nhật cuối',
    PRIMARY KEY (id),
    CONSTRAINT uk_rule_name UNIQUE (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho validation_rules
CREATE INDEX idx_enabled ON validation_rules (enabled);

-- -----------------------------------------------------------------------------
-- Table: fast_check_configs
-- Description: Cấu hình fast check cho campaign (kiểm tra nhanh trước khi validate full)
-- -----------------------------------------------------------------------------
CREATE TABLE fast_check_configs
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID tự động tăng',
    campaign_id             VARCHAR(100) NOT NULL COMMENT 'ID của campaign (unique)',
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'Trạng thái enable/disable',
    -- Time constraints
    business_hours_start    TIME COMMENT 'Giờ bắt đầu business hours',
    business_hours_end      TIME COMMENT 'Giờ kết thúc business hours',
    business_hours_timezone VARCHAR(50) COMMENT 'Timezone cho business hours',
    exclude_holidays        BOOLEAN               DEFAULT FALSE COMMENT 'Loại trừ ngày lễ',
    -- Order constraints
    min_order_value         BIGINT COMMENT 'Giá trị đơn hàng tối thiểu',
    max_order_value         BIGINT COMMENT 'Giá trị đơn hàng tối đa',
    min_items               INT COMMENT 'Số items tối thiểu trong đơn',
    max_items               INT COMMENT 'Số items tối đa trong đơn',
    -- Customer constraints
    require_all_segments    BOOLEAN               DEFAULT FALSE COMMENT 'Yêu cầu thuộc tất cả segments',
    max_order_count         INT COMMENT 'Số đơn hàng tối đa của customer',
    -- Rate limits
    max_per_hour            INT COMMENT 'Giới hạn số lần mỗi giờ',
    max_per_day             INT COMMENT 'Giới hạn số lần mỗi ngày',
    max_per_week            INT COMMENT 'Giới hạn số lần mỗi tuần',
    max_per_month           INT COMMENT 'Giới hạn số lần mỗi tháng',
    window_type             VARCHAR(20) COMMENT 'Loại window (SLIDING, FIXED)',
    created_at              TIMESTAMP    NOT NULL COMMENT 'Thời điểm tạo',
    updated_at              TIMESTAMP COMMENT 'Thời điểm cập nhật cuối',
    PRIMARY KEY (id),
    CONSTRAINT uk_campaign_id UNIQUE (campaign_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho fast_check_configs
CREATE INDEX idx_campaign_id ON fast_check_configs (campaign_id);

-- -----------------------------------------------------------------------------
-- Table: blackout_periods
-- Description: Các khoảng thời gian blackout (không cho phép validation)
-- -----------------------------------------------------------------------------
CREATE TABLE blackout_periods
(
    id                   BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID tự động tăng',
    name                 VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Tên của blackout period',
    start_time           TIMESTAMP COMMENT 'Thời điểm bắt đầu blackout',
    end_time             TIMESTAMP COMMENT 'Thời điểm kết thúc blackout',
    reason               VARCHAR(500) COMMENT 'Lý do blackout',
    fast_check_config_id BIGINT COMMENT 'ID của fast check config liên quan',
    PRIMARY KEY (id),
    CONSTRAINT fk_blackout_periods_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: flash_sale_windows
-- Description: Cửa sổ thời gian flash sale
-- -----------------------------------------------------------------------------
CREATE TABLE flash_sale_windows
(
    id                   BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID tự động tăng',
    name                 VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Tên của flash sale window',
    daily_start          TIME COMMENT 'Giờ bắt đầu hàng ngày',
    daily_end            TIME COMMENT 'Giờ kết thúc hàng ngày',
    fast_check_config_id BIGINT COMMENT 'ID của fast check config liên quan',
    PRIMARY KEY (id),
    CONSTRAINT fk_flash_sale_windows_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: flash_sale_active_days
-- Description: Các ngày active của flash sale window
-- -----------------------------------------------------------------------------
CREATE TABLE flash_sale_active_days
(
    flash_sale_window_id BIGINT NOT NULL COMMENT 'ID của flash sale window',
    active_day           VARCHAR(10) COMMENT 'Ngày trong tuần (MONDAY, TUESDAY, etc.)',
    CONSTRAINT fk_active_days_flash_sale
        FOREIGN KEY (flash_sale_window_id) REFERENCES flash_sale_windows (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: allowed_days_of_week
-- Description: Các ngày trong tuần được phép (cho fast check)
-- -----------------------------------------------------------------------------
CREATE TABLE allowed_days_of_week
(
    fast_check_config_id BIGINT NOT NULL COMMENT 'ID của fast check config',
    day_of_week          VARCHAR(10) COMMENT 'Ngày trong tuần (MONDAY, TUESDAY, etc.)',
    CONSTRAINT fk_allowed_days_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: allowed_currencies
-- Description: Các loại tiền tệ được phép
-- -----------------------------------------------------------------------------
CREATE TABLE allowed_currencies
(
    fast_check_config_id BIGINT NOT NULL COMMENT 'ID của fast check config',
    currency             VARCHAR(10) COMMENT 'Mã tiền tệ (VND, USD, EUR, etc.)',
    CONSTRAINT fk_allowed_currencies_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: required_categories
-- Description: Các categories bắt buộc phải có trong đơn hàng
-- -----------------------------------------------------------------------------
CREATE TABLE required_categories
(
    fast_check_config_id BIGINT NOT NULL COMMENT 'ID của fast check config',
    category             VARCHAR(100) COMMENT 'Mã category',
    CONSTRAINT fk_required_categories_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: excluded_categories
-- Description: Các categories bị loại trừ
-- -----------------------------------------------------------------------------
CREATE TABLE excluded_categories
(
    fast_check_config_id BIGINT NOT NULL COMMENT 'ID của fast check config',
    category             VARCHAR(100) COMMENT 'Mã category',
    CONSTRAINT fk_excluded_categories_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: allowed_segments
-- Description: Các customer segments được phép
-- -----------------------------------------------------------------------------
CREATE TABLE allowed_segments
(
    fast_check_config_id BIGINT NOT NULL COMMENT 'ID của fast check config',
    segment              VARCHAR(100) COMMENT 'Mã segment',
    CONSTRAINT fk_allowed_segments_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: excluded_segments
-- Description: Các customer segments bị loại trừ
-- -----------------------------------------------------------------------------
CREATE TABLE excluded_segments
(
    fast_check_config_id BIGINT NOT NULL COMMENT 'ID của fast check config',
    segment              VARCHAR(100) COMMENT 'Mã segment',
    CONSTRAINT fk_excluded_segments_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: allowed_regions
-- Description: Các regions được phép
-- -----------------------------------------------------------------------------
CREATE TABLE allowed_regions
(
    fast_check_config_id BIGINT NOT NULL COMMENT 'ID của fast check config',
    region               VARCHAR(100) COMMENT 'Mã region',
    CONSTRAINT fk_allowed_regions_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: excluded_regions
-- Description: Các regions bị loại trừ
-- -----------------------------------------------------------------------------
CREATE TABLE excluded_regions
(
    fast_check_config_id BIGINT NOT NULL COMMENT 'ID của fast check config',
    region               VARCHAR(100) COMMENT 'Mã region',
    CONSTRAINT fk_excluded_regions_config
        FOREIGN KEY (fast_check_config_id) REFERENCES fast_check_configs (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- -----------------------------------------------------------------------------
-- Table: time_policies
-- Description: Chính sách thời gian cho validation
-- -----------------------------------------------------------------------------
CREATE TABLE time_policies
(
    id          VARCHAR(100)                                                  NOT NULL COMMENT 'ID duy nhất của time policy',
    name        VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Tên của time policy',
    description VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Mô tả chi tiết về policy',
    active      BOOLEAN                                                       NOT NULL DEFAULT TRUE COMMENT 'Trạng thái active/inactive',
    created_at  TIMESTAMP                                                     NOT NULL COMMENT 'Thời điểm tạo',
    updated_at  TIMESTAMP COMMENT 'Thời điểm cập nhật cuối',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho time_policies
CREATE INDEX idx_time_policy_active ON time_policies (active);

-- -----------------------------------------------------------------------------
-- Table: time_windows
-- Description: Các cửa sổ thời gian của time policy
-- -----------------------------------------------------------------------------
CREATE TABLE time_windows
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID tự động tăng',
    policy_id      VARCHAR(100) NOT NULL COMMENT 'ID của time policy',
    days_of_week   VARCHAR(100) COMMENT 'Các ngày trong tuần (comma-separated)',
    start_time     TIME         NOT NULL COMMENT 'Giờ bắt đầu',
    end_time       TIME         NOT NULL COMMENT 'Giờ kết thúc',
    spans_midnight BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Có qua nửa đêm không',
    PRIMARY KEY (id),
    CONSTRAINT fk_time_windows_policy
        FOREIGN KEY (policy_id) REFERENCES time_policies (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Indexes cho time_windows
CREATE INDEX idx_time_window_policy ON time_windows (policy_id);
