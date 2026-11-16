ALTER TABLE work_order
    ADD COLUMN visit_fee_amount DECIMAL(12,2) NULL,
    ADD COLUMN visit_fee_technician_id BIGINT NULL,
    ADD COLUMN cancellation_fee_amount DECIMAL(12,2) NULL;
