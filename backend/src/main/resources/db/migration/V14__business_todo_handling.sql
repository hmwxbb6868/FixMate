-- 旧记录不补造处理依据/操作者，版本从0开始；新处理必须原子保存完整元数据。
ALTER TABLE urge_record
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN handling_conclusion VARCHAR(1000) NULL;
ALTER TABLE human_handoff_record
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN handling_conclusion VARCHAR(1000) NULL;
