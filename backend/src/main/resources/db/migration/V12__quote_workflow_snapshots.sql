-- 新报价留存真实维修人和规则指纹；旧行保持未知，不回填人员或代理来源。
ALTER TABLE quote
    ADD COLUMN technician_id BIGINT NULL,
    ADD COLUMN item_revision INT NOT NULL DEFAULT 0,
    MODIFY change_reason VARCHAR(1000) NULL;
ALTER TABLE quote_item
    ADD COLUMN content_revision INT NOT NULL DEFAULT 0,
    ADD COLUMN pricing_reason VARCHAR(1000) NULL,
    ADD COLUMN pricing_rule_fingerprint CHAR(64) NULL,
    MODIFY item_name VARCHAR(200) NOT NULL,
    MODIFY specification VARCHAR(200) NULL,
    MODIFY unit VARCHAR(32) NOT NULL,
    MODIFY pricing_clause_title VARCHAR(200) NOT NULL,
    MODIFY pricing_rule_version VARCHAR(64) NOT NULL,
    MODIFY pricing_basis_snapshot TEXT NOT NULL,
    MODIFY part_source_note VARCHAR(1000) NULL,
    MODIFY addition_reason VARCHAR(1000) NULL;
ALTER TABLE quote_override_approval
    MODIFY override_reason VARCHAR(1000) NOT NULL,
    MODIFY review_comment VARCHAR(1000) NULL;
