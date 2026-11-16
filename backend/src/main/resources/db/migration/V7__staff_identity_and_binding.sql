-- ADR-0004：STAFF认证及微信员工绑定，仅新增；无默认管理员/默认密码。
ALTER TABLE staff_user
    ADD COLUMN credential_version INT NOT NULL DEFAULT 0,
    ADD COLUMN password_change_required TINYINT(1) NOT NULL DEFAULT 0;

CREATE TABLE staff_invitation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    code_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    created_by BIGINT NOT NULL,
    consumed_at DATETIME(3) NULL,
    consumed_binding_id BIGINT NULL,
    revoked_by BIGINT NULL,
    revoked_at DATETIME(3) NULL,
    revocation_reason VARCHAR(1000) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_staff_invite_hash(code_hash),
    KEY idx_staff_invite_staff(staff_id,status,expires_at),
    CONSTRAINT fk_invitation_staff FOREIGN KEY(staff_id) REFERENCES staff_user(id),
    CONSTRAINT fk_invitation_creator FOREIGN KEY(created_by) REFERENCES staff_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE staff_wechat_binding (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    invitation_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    applicant_name VARCHAR(64) NOT NULL,
    applicant_contact_phone VARCHAR(20) NOT NULL,
    contact_phone_verified TINYINT(1) NOT NULL DEFAULT 0,
    applied_at DATETIME(3) NOT NULL,
    confirmed_by BIGINT NULL,
    confirmed_at DATETIME(3) NULL,
    rejected_by BIGINT NULL,
    rejected_at DATETIME(3) NULL,
    revoked_by BIGINT NULL,
    revoked_at DATETIME(3) NULL,
    decision_reason VARCHAR(1000) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    active_staff_id BIGINT GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING','ACTIVE') THEN staff_id ELSE NULL END) STORED,
    active_customer_id BIGINT GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING','ACTIVE') THEN customer_id ELSE NULL END) STORED,
    UNIQUE KEY uk_binding_staff_active(active_staff_id),
    UNIQUE KEY uk_binding_customer_active(active_customer_id),
    UNIQUE KEY uk_binding_invitation(invitation_id),
    KEY idx_binding_customer(customer_id,applied_at),
    CONSTRAINT fk_binding_customer FOREIGN KEY(customer_id) REFERENCES customer_user(id),
    CONSTRAINT fk_binding_staff FOREIGN KEY(staff_id) REFERENCES staff_user(id),
    CONSTRAINT fk_binding_invitation FOREIGN KEY(invitation_id) REFERENCES staff_invitation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
