-- ADR-0017: 历史不补造用户期望或同意；新工单由应用显式写CUSTOMER_SLOT。
ALTER TABLE work_order
 ADD COLUMN visit_policy VARCHAR(24) NOT NULL DEFAULT 'LEGACY_PLATFORM',
 ADD COLUMN preferred_visit_start_at DATETIME(3) NULL,
 ADD COLUMN preferred_visit_end_at DATETIME(3) NULL,
 ADD CONSTRAINT ck_order_visit_policy CHECK (visit_policy IN ('LEGACY_PLATFORM','CUSTOMER_SLOT')),
 ADD CONSTRAINT ck_order_preferred_visit CHECK (
  (preferred_visit_start_at IS NULL AND preferred_visit_end_at IS NULL)
  OR (preferred_visit_start_at IS NOT NULL AND preferred_visit_end_at IS NOT NULL AND preferred_visit_start_at<preferred_visit_end_at));
CREATE INDEX idx_appointment_technician_time ON appointment(technician_id,status,scheduled_start_at,scheduled_end_at);

-- 改约待确认时保留原预约占用，不以单方请求提前释放旧时段。
ALTER TABLE appointment
 ADD COLUMN previous_confirmed_start_at DATETIME(3) NULL,
 ADD COLUMN previous_confirmed_end_at DATETIME(3) NULL,
 ADD CONSTRAINT ck_appointment_previous_confirmed CHECK (
  (previous_confirmed_start_at IS NULL AND previous_confirmed_end_at IS NULL)
  OR (previous_confirmed_start_at IS NOT NULL AND previous_confirmed_end_at IS NOT NULL AND previous_confirmed_start_at<previous_confirmed_end_at));
