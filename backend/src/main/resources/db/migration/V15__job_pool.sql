-- ADR-0016: 旧分派保留DIRECT；不公开历史订单、不改用户状态。
ALTER TABLE work_order_assignment ADD COLUMN allocation_source VARCHAR(20) NOT NULL DEFAULT 'DIRECT', ADD CONSTRAINT ck_assignment_source CHECK (allocation_source IN ('DIRECT','SELF_CLAIM'));
CREATE TABLE job_pool_entry (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 work_order_id BIGINT NOT NULL,
 status VARCHAR(20) NOT NULL,
 published_by BIGINT NOT NULL,
 published_at DATETIME(3) NOT NULL,
 claimed_by BIGINT NULL,
 claimed_binding_id BIGINT NULL,
 claimed_binding_version INT NULL,
 assignment_id BIGINT NULL,
 claimed_at DATETIME(3) NULL,
 withdrawn_by BIGINT NULL,
 withdrawn_at DATETIME(3) NULL,
 reason VARCHAR(1000) NOT NULL,
 version INT NOT NULL DEFAULT 0,
 created_at DATETIME(3) NOT NULL,
 updated_at DATETIME(3) NOT NULL,
 published_order_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='PUBLISHED' THEN work_order_id ELSE NULL END) STORED,
 UNIQUE KEY uk_job_pool_published_order (published_order_id),
 KEY idx_job_pool_status_id (status,id),
 KEY idx_job_pool_order_id (work_order_id,id),
 CONSTRAINT fk_job_pool_order FOREIGN KEY (work_order_id) REFERENCES work_order(id),
 CONSTRAINT ck_job_pool_status CHECK (status IN ('PUBLISHED','CLAIMED','WITHDRAWN')),
 CONSTRAINT ck_job_pool_receipt CHECK (
   (status='PUBLISHED' AND claimed_binding_id IS NULL AND claimed_binding_version IS NULL AND claimed_by IS NULL AND assignment_id IS NULL AND claimed_at IS NULL AND withdrawn_by IS NULL AND withdrawn_at IS NULL)
   OR (status='CLAIMED' AND claimed_binding_id IS NOT NULL AND claimed_binding_version IS NOT NULL AND claimed_by IS NOT NULL AND assignment_id IS NOT NULL AND claimed_at IS NOT NULL AND withdrawn_by IS NULL AND withdrawn_at IS NULL)
   OR (status='WITHDRAWN' AND claimed_binding_id IS NULL AND claimed_binding_version IS NULL AND claimed_by IS NULL AND assignment_id IS NULL AND claimed_at IS NULL AND withdrawn_by IS NOT NULL AND withdrawn_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
