-- FixMate AI V1.0 阶段 1 Flyway 基线
-- 范围：非 AI 业务表（23 张）。AI 专属表（triage_session、triage_message、
--       knowledge_document、knowledge_chunk、agent_pending_action、
--       ai_eval_run、ai_eval_case_result）按设计文档 7.4 在阶段 3 追加。
-- 约定：时间一律存 UTC；金额 DECIMAL(12,2)；数量 DECIMAL(10,3)；JSON 仅存结构不固定字段。

SET NAMES utf8mb4;

-- ============ 身份与地址 ============

CREATE TABLE customer_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    openid          VARCHAR(64)  NOT NULL,
    unionid         VARCHAR(64)  NULL,
    phone           VARCHAR(20)  NULL,
    phone_verified  TINYINT(1)   NOT NULL DEFAULT 0,
    nickname        VARCHAR(64)  NULL,
    avatar_url      VARCHAR(512) NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
    last_login_at   DATETIME(3)  NULL,
    created_at      DATETIME(3)  NOT NULL,
    updated_at      DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_openid (openid),
    KEY idx_customer_unionid (unionid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE staff_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(64)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    name           VARCHAR(64)  NOT NULL,
    phone          VARCHAR(20)  NULL,
    role           VARCHAR(32)  NOT NULL,
    status         VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
    last_login_at  DATETIME(3)  NULL,
    version        INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(3)  NOT NULL,
    updated_at     DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_username (username),
    KEY idx_staff_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE customer_address (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT       NOT NULL,
    contact_name            VARCHAR(32)  NOT NULL,
    contact_phone           VARCHAR(20)  NOT NULL,
    contact_phone_verified  TINYINT(1)   NOT NULL DEFAULT 0,
    province                VARCHAR(32)  NOT NULL,
    city                    VARCHAR(32)  NOT NULL,
    district                VARCHAR(32)  NOT NULL,
    detail                  VARCHAR(200) NOT NULL,
    longitude               DECIMAL(10,7) NULL,
    latitude                DECIMAL(10,7) NULL,
    is_default              TINYINT(1)   NOT NULL DEFAULT 0,
    status                  VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
    version                 INT          NOT NULL DEFAULT 0,
    created_at              DATETIME(3)  NOT NULL,
    updated_at              DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_address_user (user_id, status),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES customer_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE file_asset (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    uploader_type  VARCHAR(16)  NOT NULL,
    uploader_id    BIGINT       NOT NULL,
    purpose        VARCHAR(32)  NOT NULL,
    bucket         VARCHAR(64)  NOT NULL,
    storage_key    VARCHAR(256) NOT NULL,
    etag           VARCHAR(64)  NULL,
    original_name  VARCHAR(256) NULL,
    mime_type      VARCHAR(64)  NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    sha256         CHAR(64)     NOT NULL,
    status         VARCHAR(16)  NOT NULL DEFAULT 'TEMP',
    created_at     DATETIME(3)  NOT NULL,
    updated_at     DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_storage_key (bucket, storage_key),
    KEY idx_file_uploader (uploader_type, uploader_id),
    KEY idx_file_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============ 设备、服务、价格 ============

CREATE TABLE appliance_brand (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    code        VARCHAR(32) NOT NULL,
    name        VARCHAR(64) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    sort_order  INT         NOT NULL DEFAULT 0,
    created_at  DATETIME(3) NOT NULL,
    updated_at  DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_brand_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE appliance_model (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    brand_id               BIGINT       NOT NULL,
    appliance_type         VARCHAR(32)  NOT NULL,
    model_code             VARCHAR(64)  NOT NULL,
    normalized_model_code  VARCHAR(64)  NOT NULL,
    display_name           VARCHAR(128) NULL,
    official_product_url   VARCHAR(512) NULL,
    verified_at            DATETIME(3)  NULL,
    source_status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING_SOURCE',
    coverage_status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING_SOURCE',
    status                 VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
    created_at             DATETIME(3)  NOT NULL,
    updated_at             DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_brand_code (brand_id, appliance_type, model_code),
    KEY idx_model_normalized (normalized_model_code),
    KEY idx_model_coverage (coverage_status),
    CONSTRAINT fk_model_brand FOREIGN KEY (brand_id) REFERENCES appliance_brand (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service_item (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(64)  NOT NULL,
    appliance_type  VARCHAR(32)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(512) NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME(3)  NOT NULL,
    updated_at      DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_item_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pricing_rule (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    clause_no        VARCHAR(64)   NOT NULL,
    rule_version     VARCHAR(32)   NOT NULL,
    service_item_id  BIGINT        NOT NULL,
    fee_type         VARCHAR(16)   NOT NULL,
    title            VARCHAR(128)  NOT NULL,
    description      VARCHAR(512)  NULL,
    unit             VARCHAR(16)   NOT NULL,
    fixed_price      DECIMAL(12,2) NULL,
    min_price        DECIMAL(12,2) NULL,
    max_price        DECIMAL(12,2) NULL,
    refund_policy    VARCHAR(256)  NULL,
    effective_from   DATETIME(3)   NOT NULL,
    effective_to     DATETIME(3)   NULL,
    status           VARCHAR(16)   NOT NULL DEFAULT 'ENABLED',
    version          INT           NOT NULL DEFAULT 0,
    created_by       BIGINT        NULL,
    updated_by       BIGINT        NULL,
    created_at       DATETIME(3)   NOT NULL,
    updated_at       DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pricing_clause_version (clause_no, rule_version),
    KEY idx_pricing_service (service_item_id, status),
    CONSTRAINT fk_pricing_service FOREIGN KEY (service_item_id) REFERENCES service_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============ 报修申请、媒体、安全 ============

CREATE TABLE repair_request (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    repair_no           VARCHAR(32)   NOT NULL,
    user_id             BIGINT        NOT NULL,
    appliance_type      VARCHAR(32)   NULL,
    brand_id            BIGINT        NULL,
    model_id            BIGINT        NULL,
    model_text          VARCHAR(128)  NULL,
    serial_number       VARCHAR(64)   NULL,
    symptom_description VARCHAR(1000) NULL,
    error_code_text     VARCHAR(64)   NULL,
    occurred_at         DATETIME(3)   NULL,
    address_id          BIGINT        NULL,
    address_snapshot    JSON          NULL,
    contact_snapshot    JSON          NULL,
    safety_answers      JSON          NULL,
    status              VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    risk_flags          JSON          NULL,
    coverage_status     VARCHAR(32)   NULL,
    triage_session_id   BIGINT        NULL,
    work_order_id       BIGINT        NULL,
    version             INT           NOT NULL DEFAULT 0,
    created_at          DATETIME(3)   NOT NULL,
    updated_at          DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repair_no (repair_no),
    KEY idx_repair_user (user_id, created_at),
    KEY idx_repair_status (status),
    KEY idx_repair_brand_model (brand_id, model_id),
    CONSTRAINT fk_repair_user FOREIGN KEY (user_id) REFERENCES customer_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE repair_request_media (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    repair_request_id  BIGINT      NOT NULL,
    file_id            BIGINT      NOT NULL,
    media_purpose      VARCHAR(32) NOT NULL,
    sort_order         INT         NOT NULL DEFAULT 0,
    created_at         DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repair_media (repair_request_id, file_id),
    KEY idx_repair_media_file (file_id),
    CONSTRAINT fk_repair_media_request FOREIGN KEY (repair_request_id) REFERENCES repair_request (id),
    CONSTRAINT fk_repair_media_file FOREIGN KEY (file_id) REFERENCES file_asset (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE safety_rule (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(32)  NOT NULL,
    title           VARCHAR(64)  NOT NULL,
    template_text   VARCHAR(1000) NOT NULL,
    keywords        JSON         NOT NULL,
    appliance_types JSON         NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME(3)  NOT NULL,
    updated_at      DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_safety_rule_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE safety_event (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    repair_request_id  BIGINT        NOT NULL,
    rule_codes         JSON          NOT NULL,
    matched_rules      JSON          NOT NULL,
    matched_text       VARCHAR(1000) NULL,
    status             VARCHAR(16)   NOT NULL DEFAULT 'OPEN',
    work_order_id      BIGINT        NULL,
    acknowledged_by    BIGINT        NULL,
    acknowledged_at    DATETIME(3)   NULL,
    resolved_by        BIGINT        NULL,
    resolved_at        DATETIME(3)   NULL,
    created_at         DATETIME(3)   NOT NULL,
    updated_at         DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_safety_status (status, created_at),
    KEY idx_safety_repair (repair_request_id),
    CONSTRAINT fk_safety_repair FOREIGN KEY (repair_request_id) REFERENCES repair_request (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============ 工单、预约、人工动作 ============

CREATE TABLE work_order (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    work_order_no       VARCHAR(32)   NOT NULL,
    repair_request_id   BIGINT        NOT NULL,
    customer_id         BIGINT        NOT NULL,
    status              VARCHAR(32)   NOT NULL,
    appliance_type      VARCHAR(32)   NOT NULL,
    brand_code          VARCHAR(32)   NULL,
    model_code          VARCHAR(64)   NULL,
    device_snapshot     JSON          NULL,
    contact_snapshot    JSON          NULL,
    address_snapshot    JSON          NULL,
    symptom_description VARCHAR(1000) NULL,
    technician_id       BIGINT        NULL,
    assigned_at         DATETIME(3)   NULL,
    version             INT           NOT NULL DEFAULT 0,
    created_at          DATETIME(3)   NOT NULL,
    updated_at          DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_no (work_order_no),
    UNIQUE KEY uk_work_order_repair (repair_request_id),
    KEY idx_work_order_customer (customer_id, created_at),
    KEY idx_work_order_status (status),
    KEY idx_work_order_technician (technician_id, status),
    CONSTRAINT fk_work_order_repair FOREIGN KEY (repair_request_id) REFERENCES repair_request (id),
    CONSTRAINT fk_work_order_customer FOREIGN KEY (customer_id) REFERENCES customer_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE work_order_status_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT       NOT NULL,
    from_status   VARCHAR(32)  NULL,
    to_status     VARCHAR(32)  NOT NULL,
    reason        VARCHAR(500) NULL,
    operator_type VARCHAR(16)  NOT NULL,
    operator_id   BIGINT       NULL,
    trace_id      VARCHAR(64)  NULL,
    created_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_status_log_order (work_order_id, created_at),
    CONSTRAINT fk_status_log_order FOREIGN KEY (work_order_id) REFERENCES work_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE appointment (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    work_order_id      BIGINT       NOT NULL,
    technician_id      BIGINT       NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    scheduled_start_at DATETIME(3)  NOT NULL,
    scheduled_end_at   DATETIME(3)  NOT NULL,
    note               VARCHAR(500) NULL,
    version            INT          NOT NULL DEFAULT 0,
    created_at         DATETIME(3)  NOT NULL,
    updated_at         DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_appointment_order (work_order_id),
    KEY idx_appointment_status_time (status, scheduled_end_at),
    CONSTRAINT fk_appointment_order FOREIGN KEY (work_order_id) REFERENCES work_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE urge_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    reason        VARCHAR(200) NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    handled_by    BIGINT       NULL,
    handled_at    DATETIME(3)  NULL,
    created_at    DATETIME(3)  NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_urge_status (status, created_at),
    KEY idx_urge_order (work_order_id),
    CONSTRAINT fk_urge_order FOREIGN KEY (work_order_id) REFERENCES work_order (id),
    CONSTRAINT fk_urge_user FOREIGN KEY (user_id) REFERENCES customer_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE human_handoff_record (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    work_order_id BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    handled_by    BIGINT       NULL,
    handled_at    DATETIME(3)  NULL,
    created_at    DATETIME(3)  NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_handoff_status (status, created_at),
    KEY idx_handoff_order (work_order_id),
    CONSTRAINT fk_handoff_order FOREIGN KEY (work_order_id) REFERENCES work_order (id),
    CONSTRAINT fk_handoff_user FOREIGN KEY (user_id) REFERENCES customer_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service_record (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    work_order_id     BIGINT       NOT NULL,
    technician_id     BIGINT       NOT NULL,
    inspection_facts  JSON         NOT NULL,
    summary           VARCHAR(1000) NULL,
    submitted_by      BIGINT       NOT NULL,
    submitted_at      DATETIME(3)  NOT NULL,
    created_at        DATETIME(3)  NOT NULL,
    updated_at        DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_service_record_order (work_order_id),
    CONSTRAINT fk_service_record_order FOREIGN KEY (work_order_id) REFERENCES work_order (id),
    CONSTRAINT fk_service_record_tech FOREIGN KEY (technician_id) REFERENCES staff_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============ 报价 ============

CREATE TABLE quote (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    quote_no                  VARCHAR(32)   NOT NULL,
    work_order_id             BIGINT        NOT NULL,
    quote_type                VARCHAR(16)   NOT NULL,
    version_no                INT           NOT NULL,
    previous_quote_id         BIGINT        NULL,
    base_quote_id             BIGINT        NULL,
    status                    VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    currency                  CHAR(3)       NOT NULL DEFAULT 'CNY',
    subtotal_amount           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    confirmed_amount_before   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount              DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    change_reason             VARCHAR(500)  NULL,
    submission_source         VARCHAR(32)   NULL,
    service_record_id         BIGINT        NULL,
    accepted_view_snapshot    JSON          NULL,
    created_by                BIGINT        NOT NULL,
    submitted_by              BIGINT        NULL,
    submitted_at              DATETIME(3)   NULL,
    user_decided_at           DATETIME(3)   NULL,
    decision_reason           VARCHAR(200)  NULL,
    expires_at                DATETIME(3)   NULL,
    version                   INT           NOT NULL DEFAULT 0,
    created_at                DATETIME(3)   NOT NULL,
    updated_at                DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quote_no (quote_no),
    UNIQUE KEY uk_quote_order_version (work_order_id, version_no),
    KEY idx_quote_order (work_order_id),
    KEY idx_quote_status (status, expires_at),
    CONSTRAINT fk_quote_order FOREIGN KEY (work_order_id) REFERENCES work_order (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quote_item (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    quote_id                BIGINT        NOT NULL,
    fee_type                VARCHAR(16)   NOT NULL,
    service_item_id         BIGINT        NULL,
    item_name               VARCHAR(128)  NOT NULL,
    specification           VARCHAR(128)  NULL,
    unit                    VARCHAR(16)   NOT NULL,
    unit_price              DECIMAL(12,2) NOT NULL,
    quantity                DECIMAL(10,3) NOT NULL,
    amount                  DECIMAL(12,2) NOT NULL,
    pricing_rule_id         BIGINT        NOT NULL,
    pricing_clause_no       VARCHAR(64)   NOT NULL,
    pricing_rule_version    VARCHAR(32)   NOT NULL,
    pricing_basis_snapshot  VARCHAR(1000) NOT NULL,
    addition_reason         VARCHAR(500)  NULL,
    sort_order              INT           NOT NULL DEFAULT 0,
    created_by              BIGINT        NOT NULL,
    created_at              DATETIME(3)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_quote_item_quote (quote_id),
    KEY idx_quote_item_rule (pricing_rule_id),
    CONSTRAINT fk_quote_item_quote FOREIGN KEY (quote_id) REFERENCES quote (id),
    CONSTRAINT fk_quote_item_rule FOREIGN KEY (pricing_rule_id) REFERENCES pricing_rule (id),
    CONSTRAINT ck_quote_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quote_override_approval (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    quote_id           BIGINT      NOT NULL,
    requested_by       BIGINT      NOT NULL,
    quote_content_hash CHAR(64)    NOT NULL,
    status             VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    override_reason    VARCHAR(500) NOT NULL,
    reviewed_by        BIGINT      NULL,
    reviewed_at        DATETIME(3) NULL,
    review_comment     VARCHAR(500) NULL,
    invalidated_at     DATETIME(3) NULL,
    version            INT         NOT NULL DEFAULT 0,
    created_at         DATETIME(3) NOT NULL,
    updated_at         DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_override_quote (quote_id),
    KEY idx_override_status (status),
    CONSTRAINT fk_override_quote FOREIGN KEY (quote_id) REFERENCES quote (id),
    CONSTRAINT ck_override_not_self CHECK (reviewed_by IS NULL OR reviewed_by <> requested_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============ 审计与幂等 ============

CREATE TABLE audit_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    actor_type   VARCHAR(16)  NOT NULL,
    actor_id     BIGINT       NULL,
    action       VARCHAR(64)  NOT NULL,
    target_type  VARCHAR(32)  NOT NULL,
    target_id    VARCHAR(64)  NULL,
    before_state JSON         NULL,
    after_state  JSON         NULL,
    trace_id     VARCHAR(64)  NULL,
    ip           VARCHAR(64)  NULL,
    created_at   DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_target (target_type, target_id),
    KEY idx_audit_actor (actor_type, actor_id, created_at),
    KEY idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE idempotency_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    scope_key       VARCHAR(160) NOT NULL,
    idempotency_key VARCHAR(64)  NOT NULL,
    operation       VARCHAR(80)  NOT NULL,
    request_hash    CHAR(64)     NOT NULL,
    response_status INT          NOT NULL,
    response_body   MEDIUMTEXT   NULL,
    sensitive_flag  TINYINT(1)   NOT NULL DEFAULT 0,
    expires_at      DATETIME(3)  NOT NULL,
    created_at      DATETIME(3)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idem_scope_key (scope_key, idempotency_key),
    KEY idx_idem_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
