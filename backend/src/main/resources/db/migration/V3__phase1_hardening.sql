-- FixMate AI 阶段 1 加固迁移
-- 1) 幂等记录支持原子抢占（PENDING → COMPLETED）与 family 绑定
-- 2) 报价：每工单最多一个 SUBMITTED 报价的可执行唯一约束
-- 3) 报价行项目补计价条款标题（冻结规则要求用户可见）

-- ---------- 幂等 ----------
ALTER TABLE idempotency_record
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING' AFTER idempotency_key,
    ADD COLUMN family_id VARCHAR(64) NULL AFTER scope_key,
    MODIFY COLUMN response_status INT NULL,
    MODIFY COLUMN response_body MEDIUMTEXT NULL;

CREATE INDEX idx_idem_family ON idempotency_record (family_id);

-- ---------- 报价行项目 ----------
ALTER TABLE quote_item
    ADD COLUMN pricing_clause_title VARCHAR(128) NULL AFTER pricing_clause_no;

UPDATE quote_item
SET pricing_clause_title = pricing_clause_no
WHERE pricing_clause_title IS NULL;

ALTER TABLE quote_item
    MODIFY COLUMN pricing_clause_title VARCHAR(128) NOT NULL;

-- ---------- 每工单最多一个 SUBMITTED 报价 ----------
ALTER TABLE quote
    ADD COLUMN submitted_work_order_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status = 'SUBMITTED' THEN work_order_id ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_quote_one_submitted (submitted_work_order_id);
