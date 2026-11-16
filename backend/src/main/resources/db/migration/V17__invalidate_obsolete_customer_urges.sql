-- Preserve the original request and any previously recorded staff handling.
UPDATE urge_record u
JOIN work_order w ON w.id = u.work_order_id
SET u.status = 'INVALIDATED',
    u.handled_by = NULL,
    u.handled_at = UTC_TIMESTAMP(3),
    u.handling_conclusion = CASE
        WHEN w.status IN ('COMPLETED', 'CANCELLED', 'CLOSED_UNRESOLVED') THEN '关联工单已结束，催办自动失效'
        ELSE '接单大厅发布已撤回，催办自动失效'
    END,
    u.version = u.version + 1,
    u.updated_at = UTC_TIMESTAMP(3)
WHERE u.status = 'PENDING'
  AND (
    w.status IN ('COMPLETED', 'CANCELLED', 'CLOSED_UNRESOLVED')
    OR EXISTS (
        SELECT 1 FROM job_pool_entry p
        WHERE p.work_order_id = w.id AND p.withdrawn_at >= u.created_at
    )
    OR (
        w.status = 'WAITING_ASSIGNMENT'
        AND w.current_assignment_id IS NULL AND w.technician_id IS NULL AND w.takeover_by IS NULL
        AND 'WITHDRAWN' = (
            SELECT p.status FROM job_pool_entry p WHERE p.work_order_id = w.id ORDER BY p.id DESC LIMIT 1
        )
    )
  );
