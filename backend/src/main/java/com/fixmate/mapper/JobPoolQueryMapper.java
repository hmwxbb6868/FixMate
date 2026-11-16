package com.fixmate.mapper;
import com.fixmate.entity.JobPoolEntryEntity;
import java.util.List;
import org.apache.ibatis.annotations.*;
@Mapper public interface JobPoolQueryMapper {
 String ORDER_ELIGIBLE="""
 w.visit_policy='CUSTOMER_SLOT' AND w.preferred_visit_start_at>UTC_TIMESTAMP(3) AND w.preferred_visit_end_at>w.preferred_visit_start_at
 AND w.preferred_visit_start_at<=DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 30 DAY)
 AND w.preferred_visit_end_at<=DATE_ADD(w.preferred_visit_start_at,INTERVAL 4 HOUR)
 AND w.status='WAITING_ASSIGNMENT'
 AND w.current_assignment_id IS NULL AND w.technician_id IS NULL AND w.takeover_by IS NULL
 AND r.work_order_id=w.id AND r.user_id=w.customer_id
 AND r.service_contact_snapshot IS NOT NULL
 AND JSON_UNQUOTE(JSON_EXTRACT(r.service_contact_snapshot,'$.contactSource')) IN ('SUBMISSION_SNAPSHOT','MANUAL_REVIEW_SNAPSHOT')
 AND JSON_TYPE(JSON_EXTRACT(r.service_contact_snapshot,'$.contactName'))='STRING'
 AND JSON_TYPE(JSON_EXTRACT(r.service_contact_snapshot,'$.contactPhone'))='STRING'
 AND LENGTH(TRIM(JSON_UNQUOTE(JSON_EXTRACT(r.service_contact_snapshot,'$.contactName'))))>0
 AND JSON_UNQUOTE(JSON_EXTRACT(r.service_contact_snapshot,'$.contactPhone')) REGEXP '^[0-9+() -]{6,20}$'
 AND LENGTH(REGEXP_REPLACE(JSON_UNQUOTE(JSON_EXTRACT(r.service_contact_snapshot,'$.contactPhone')),'[^0-9]',''))>=6
 AND (
     (JSON_TYPE(JSON_EXTRACT(COALESCE(r.address_snapshot,w.address_snapshot),'$.province'))='STRING'
      AND LENGTH(TRIM(JSON_UNQUOTE(JSON_EXTRACT(COALESCE(r.address_snapshot,w.address_snapshot),'$.province'))))>0)
  OR (JSON_TYPE(JSON_EXTRACT(COALESCE(r.address_snapshot,w.address_snapshot),'$.city'))='STRING'
      AND LENGTH(TRIM(JSON_UNQUOTE(JSON_EXTRACT(COALESCE(r.address_snapshot,w.address_snapshot),'$.city'))))>0)
  OR (JSON_TYPE(JSON_EXTRACT(COALESCE(r.address_snapshot,w.address_snapshot),'$.district'))='STRING'
      AND LENGTH(TRIM(JSON_UNQUOTE(JSON_EXTRACT(COALESCE(r.address_snapshot,w.address_snapshot),'$.district'))))>0)
  OR (JSON_TYPE(JSON_EXTRACT(COALESCE(r.address_snapshot,w.address_snapshot),'$.detail'))='STRING'
      AND LENGTH(TRIM(JSON_UNQUOTE(JSON_EXTRACT(COALESCE(r.address_snapshot,w.address_snapshot),'$.detail'))))>0)
 )
 AND NOT EXISTS (SELECT 1 FROM safety_event s WHERE s.work_order_id=w.id AND s.status<>'RESOLVED')
 AND NOT EXISTS (SELECT 1 FROM work_order_assignment a WHERE a.work_order_id=w.id AND a.status IN ('PENDING','ACTIVE','RETURN_REQUESTED'))
 """;
 String ELIGIBLE="""
 FROM job_pool_entry p JOIN work_order w ON w.id=p.work_order_id
 JOIN repair_request r ON r.id=w.repair_request_id
 WHERE p.status='PUBLISHED' AND
 """ + ORDER_ELIGIBLE;
 @Select("SELECT p.* "+ELIGIBLE+" ORDER BY p.id DESC LIMIT #{size} OFFSET #{offset}")
 List<JobPoolEntryEntity> page(@Param("offset") long offset,@Param("size") int size);
 @Select("SELECT COUNT(*) "+ELIGIBLE) long count();
}
