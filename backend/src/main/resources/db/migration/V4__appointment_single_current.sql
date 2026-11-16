-- FixMate AI 阶段 1 加固迁移（审计 #10）
-- 同一工单同一时间只能有一条"当前预约"（PENDING/CONFIRMED/RESCHEDULE_REQUESTED）。
-- 已完成、已取消、爽约的预约不受限制，便于重新预约与留档。
-- MySQL 不支持部分唯一索引，用生成列把"当前预约"映射为 work_order_id，其余置 NULL。

ALTER TABLE appointment
    ADD COLUMN current_work_order_id BIGINT
        GENERATED ALWAYS AS (
            CASE WHEN status IN ('PENDING', 'CONFIRMED', 'RESCHEDULE_REQUESTED')
                 THEN work_order_id
                 ELSE NULL
            END
        ) STORED,
    ADD UNIQUE KEY uk_appointment_current (current_work_order_id);
