-- ADR-0005/0008：新增人工核对与私有联系快照；既有脱敏快照不改写，旧数据留NULL。
ALTER TABLE repair_request
    ADD COLUMN service_contact_snapshot JSON NULL,
    ADD COLUMN manual_reviewed_by BIGINT NULL,
    ADD COLUMN manual_reviewed_at DATETIME(3) NULL,
    ADD COLUMN manual_review_note VARCHAR(1000) NULL;
