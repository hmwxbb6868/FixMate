-- FixMate AI 阶段 1 加固迁移（审计 #14、#17）
-- 1) 验收备注与验收时间必须可持久化：详情页不能只靠审计日志重建
-- 2) 配件费用必须保存来源说明（冻结规则 10.3），契约字段为 partSourceNote

ALTER TABLE work_order
    ADD COLUMN acceptance_note VARCHAR(500) NULL AFTER symptom_description,
    ADD COLUMN accepted_at DATETIME(3) NULL AFTER acceptance_note;

ALTER TABLE quote_item
    ADD COLUMN part_source_note VARCHAR(512) NULL AFTER pricing_basis_snapshot;

-- 存量配件行没有来源说明时置为"历史数据未记录"，保留可读性且不伪装成已填写
UPDATE quote_item
SET part_source_note = '历史数据未记录'
WHERE fee_type = 'PART' AND part_source_note IS NULL;
