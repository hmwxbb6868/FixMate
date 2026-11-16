-- ADR-0005/0010：记录只追加，旧记录按原已提交检测事实兼容，不伪造时间或提交者。
ALTER TABLE service_record
    ADD COLUMN record_kind VARCHAR(16) NOT NULL DEFAULT 'INSPECTION',
    ADD COLUMN corrects_record_id BIGINT NULL,
    ADD UNIQUE KEY uk_service_record_correction(corrects_record_id),
    ADD KEY idx_service_record_order_kind(work_order_id,record_kind,submitted_at),
    ADD CONSTRAINT fk_service_record_correction FOREIGN KEY(corrects_record_id) REFERENCES service_record(id);
