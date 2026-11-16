-- Repair only previously released safety orders that still have no usable slot or active dispatch.
START TRANSACTION;
SET @fixmate_v18_visit_time = UTC_TIMESTAMP(3);
SET @fixmate_v18_visit_reason = '历史安全报修解除后尚未选定有效上门时段';

INSERT INTO work_order_status_log
    (work_order_id, from_status, to_status, reason, operator_type, created_at)
SELECT w.id, w.status, 'WAITING_CUSTOMER_VISIT', @fixmate_v18_visit_reason,
       'SYSTEM', @fixmate_v18_visit_time
FROM work_order w
JOIN repair_request r ON r.id=w.repair_request_id AND r.work_order_id=w.id AND r.user_id=w.customer_id
LEFT JOIN work_order_status_log inspection ON inspection.work_order_id=w.id AND inspection.to_status='IN_INSPECTION'
WHERE w.status='WAITING_ASSIGNMENT' AND r.status='HIGH_RISK_BLOCKED'
  AND inspection.id IS NULL
  AND w.current_assignment_id IS NULL AND w.technician_id IS NULL AND w.takeover_by IS NULL
  AND (w.preferred_visit_start_at IS NULL OR w.preferred_visit_end_at IS NULL
       OR w.preferred_visit_start_at<=@fixmate_v18_visit_time
       OR w.preferred_visit_start_at>DATE_ADD(@fixmate_v18_visit_time, INTERVAL 30 DAY)
       OR w.preferred_visit_end_at<=w.preferred_visit_start_at
       OR w.preferred_visit_end_at>DATE_ADD(w.preferred_visit_start_at, INTERVAL 4 HOUR))
  AND EXISTS (SELECT 1 FROM safety_event s WHERE s.work_order_id=w.id AND s.repair_request_id=r.id AND s.status='RESOLVED')
  AND NOT EXISTS (SELECT 1 FROM safety_event s WHERE s.work_order_id=w.id AND s.status<>'RESOLVED')
  AND NOT EXISTS (SELECT 1 FROM work_order_assignment a WHERE a.work_order_id=w.id AND a.status IN ('PENDING','ACTIVE','RETURN_REQUESTED'))
  AND NOT EXISTS (SELECT 1 FROM job_pool_entry p WHERE p.work_order_id=w.id AND p.status='PUBLISHED')
  AND NOT EXISTS (SELECT 1 FROM appointment a WHERE a.work_order_id=w.id AND a.status IN ('PENDING','CONFIRMED','RESCHEDULE_REQUESTED'))
  AND NOT EXISTS (SELECT 1 FROM service_record s WHERE s.work_order_id=w.id)
  AND NOT EXISTS (SELECT 1 FROM quote q WHERE q.work_order_id=w.id);

UPDATE work_order w
JOIN work_order_status_log s ON s.work_order_id=w.id AND s.from_status='WAITING_ASSIGNMENT'
    AND s.to_status='WAITING_CUSTOMER_VISIT' AND s.created_at=@fixmate_v18_visit_time AND s.reason=@fixmate_v18_visit_reason
SET w.status='WAITING_CUSTOMER_VISIT', w.visit_policy='CUSTOMER_SLOT',
    w.preferred_visit_start_at=NULL, w.preferred_visit_end_at=NULL,
    w.version=w.version+1, w.updated_at=@fixmate_v18_visit_time
WHERE w.status='WAITING_ASSIGNMENT';
COMMIT;
