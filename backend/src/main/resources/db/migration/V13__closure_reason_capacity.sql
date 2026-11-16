-- 对齐已有1000字业务原因契约，不改历史事实、金额、状态或分派。
ALTER TABLE work_order_status_log MODIFY reason VARCHAR(1000) NULL;
ALTER TABLE human_handoff_record MODIFY reason VARCHAR(1000) NOT NULL;
