package com.fixmate.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminFollowUpQueryMapper {
    record Row(String kind,Long targetId,String referenceNo,String waitingFor,LocalDateTime waitingSince,long categoryTotal) {}

    @ConstructorArgs({
        @Arg(column="kind",javaType=String.class), @Arg(column="target_id",javaType=Long.class),
        @Arg(column="reference_no",javaType=String.class), @Arg(column="waiting_for",javaType=String.class),
        @Arg(column="waiting_since",javaType=LocalDateTime.class), @Arg(column="category_total",javaType=long.class)
    })
    @Select("""
        WITH candidates AS (
            SELECT 'REPAIR_REVIEW' AS kind, r.id AS target_id, r.repair_no AS reference_no,
                   'STAFF' AS waiting_for, r.updated_at AS waiting_since, #{shortCutoff} AS cutoff
            FROM repair_request r
            WHERE r.status IN ('SUBMITTED','HUMAN_HANDOFF') AND r.work_order_id IS NULL AND r.triage_session_id IS NULL
              AND NOT EXISTS (SELECT 1 FROM work_order w WHERE w.repair_request_id=r.id AND w.customer_id=r.user_id)
              AND NOT EXISTS (SELECT 1 FROM safety_event se WHERE se.repair_request_id=r.id AND se.status<>'RESOLVED')
            UNION ALL
            SELECT 'CUSTOMER_CONFIRM', r.id, r.repair_no, 'CUSTOMER',
                   COALESCE(r.manual_reviewed_at,r.updated_at), #{longCutoff}
            FROM repair_request r
            WHERE r.status='TRIAGED' AND r.work_order_id IS NULL
              AND NOT EXISTS (SELECT 1 FROM work_order w WHERE w.repair_request_id=r.id AND w.customer_id=r.user_id)
              AND NOT EXISTS (SELECT 1 FROM safety_event se WHERE se.repair_request_id=r.id AND se.status<>'RESOLVED')
            UNION ALL
            SELECT 'DISPATCH', w.id, w.work_order_no,
                   CASE
                     WHEN w.visit_policy='CUSTOMER_SLOT'
                       AND (w.preferred_visit_start_at IS NULL OR w.preferred_visit_start_at<=#{now}
                            OR w.preferred_visit_end_at IS NULL OR w.preferred_visit_end_at<=w.preferred_visit_start_at)
                       THEN 'CUSTOMER'
                     WHEN a.status='PENDING' OR p.status='PUBLISHED' THEN 'TECHNICIAN'
                     ELSE 'STAFF'
                   END,
                   CASE WHEN a.status='PENDING' THEN a.assigned_at
                        WHEN p.status='PUBLISHED' THEN p.published_at
                        ELSE w.updated_at END, #{shortCutoff}
            FROM work_order w
            LEFT JOIN work_order_assignment a ON a.id=w.current_assignment_id AND a.work_order_id=w.id
            LEFT JOIN job_pool_entry p ON p.work_order_id=w.id AND p.status='PUBLISHED'
            WHERE w.status='WAITING_ASSIGNMENT' AND w.takeover_by IS NULL
              AND (a.status='PENDING' OR (w.current_assignment_id IS NULL AND w.technician_id IS NULL))
            UNION ALL
            SELECT 'APPOINTMENT', w.id, w.work_order_no,
                   CASE WHEN ap.status='MISSED' THEN 'STAFF' ELSE 'TECHNICIAN' END,
                   ap.scheduled_end_at, #{shortCutoff}
            FROM work_order w
            JOIN appointment ap ON ap.work_order_id=w.id
              AND ap.id=(SELECT MAX(other.id) FROM appointment other WHERE other.work_order_id=w.id)
            WHERE (w.status='APPOINTED' AND ap.status='CONFIRMED')
               OR (w.status='WAITING_APPOINTMENT' AND ap.status='MISSED')
            UNION ALL
            SELECT 'QUOTE_CONFIRM', w.id, w.work_order_no,
                   CASE WHEN q.expires_at<=#{now} THEN 'STAFF' ELSE 'CUSTOMER' END,
                   q.submitted_at, #{quoteCutoff}
            FROM work_order w
            JOIN quote q ON q.id=(SELECT newest.id FROM quote newest
                WHERE newest.work_order_id=w.id AND newest.status='SUBMITTED'
                ORDER BY newest.version_no DESC LIMIT 1)
            WHERE w.status IN ('WAITING_QUOTE_CONFIRMATION','PAUSED_FOR_REQUOTE')
            UNION ALL
            SELECT 'ACCEPTANCE', w.id, w.work_order_no, 'CUSTOMER',
                   COALESCE((SELECT MAX(s.created_at) FROM work_order_status_log s
                       WHERE s.work_order_id=w.id AND s.to_status='WAITING_ACCEPTANCE'),w.updated_at), #{longCutoff}
            FROM work_order w WHERE w.status='WAITING_ACCEPTANCE'
        ), ranked AS (
            SELECT kind,target_id,reference_no,waiting_for,waiting_since,
                   ROW_NUMBER() OVER (PARTITION BY kind ORDER BY waiting_since,target_id) AS sequence_no,
                   COUNT(*) OVER (PARTITION BY kind) AS category_total
            FROM candidates c WHERE waiting_since<=cutoff
              AND (kind IN ('REPAIR_REVIEW','CUSTOMER_CONFIRM') OR NOT EXISTS
                (SELECT 1 FROM safety_event se WHERE se.work_order_id=c.target_id AND se.status<>'RESOLVED'))
        )
        SELECT kind,target_id,reference_no,waiting_for,waiting_since,category_total
        FROM ranked WHERE sequence_no<=5 ORDER BY waiting_since,target_id,kind
        """)
    List<Row> overdue(@Param("now") LocalDateTime now,@Param("shortCutoff") LocalDateTime shortCutoff,
                      @Param("quoteCutoff") LocalDateTime quoteCutoff,@Param("longCutoff") LocalDateTime longCutoff);
}
