START TRANSACTION;
SET @fixmate_v21_time = UTC_TIMESTAMP(3);
SET @fixmate_v21_reason = '客户已拒绝追加报价，等待平台协调';

INSERT INTO work_order_status_log (work_order_id, from_status, to_status, reason, operator_type, created_at)
SELECT w.id, 'IN_REPAIR', 'WAITING_MANUAL_REVIEW', @fixmate_v21_reason, 'SYSTEM', @fixmate_v21_time
FROM work_order w
WHERE w.status = 'IN_REPAIR'
  AND EXISTS (
      SELECT 1 FROM quote q
      WHERE q.work_order_id = w.id AND q.quote_type = 'ADDITIONAL'
        AND q.status = 'REJECTED' AND q.user_decided_at IS NOT NULL
        AND q.version_no > COALESCE((
            SELECT MAX(a.version_no) FROM quote a
            WHERE a.work_order_id = w.id AND a.status = 'ACCEPTED'
        ), 0)
  );

UPDATE work_order w
JOIN work_order_status_log s ON s.work_order_id = w.id AND s.created_at = @fixmate_v21_time
    AND s.from_status = 'IN_REPAIR' AND s.to_status = 'WAITING_MANUAL_REVIEW'
    AND s.operator_type = 'SYSTEM' AND s.reason = @fixmate_v21_reason
SET w.status = 'WAITING_MANUAL_REVIEW', w.version = w.version + 1, w.updated_at = @fixmate_v21_time
WHERE w.status = 'IN_REPAIR';
COMMIT;
