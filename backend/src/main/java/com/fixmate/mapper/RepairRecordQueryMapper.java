package com.fixmate.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fixmate.dto.user.RepairRecordListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RepairRecordQueryMapper {
    @Select("""
        <script>
        SELECT r.id, r.repair_no, r.status AS repair_status, r.appliance_type,
               b.code AS brand_code, COALESCE(m.model_code,r.model_text) AS model_text,
               LEFT(r.symptom_description,120) AS symptom_summary, r.created_at,
               w.id AS work_order_id, w.status AS work_order_status, r.version
        FROM repair_request r
        LEFT JOIN work_order w ON w.id=r.work_order_id AND w.customer_id=r.user_id AND w.repair_request_id=r.id
        LEFT JOIN appliance_brand b ON b.id=r.brand_id
        LEFT JOIN appliance_model m ON m.id=r.model_id
        WHERE r.user_id=#{userId}
        <if test="group != 'DRAFT'">AND r.status != 'DRAFT'</if>
        <choose>
          <when test="group == 'DRAFT'">AND r.status='DRAFT'</when>
          <when test="group == 'ENDED'">
            AND (r.status='CANCELLED' OR w.status IN ('COMPLETED','CANCELLED','CLOSED_UNRESOLVED'))
          </when>
          <when test="group == 'PROCESSING'">
            AND r.status != 'CANCELLED'
            AND (w.status IN ('APPOINTED','IN_INSPECTION','WAITING_QUOTE','WAITING_QUOTE_CONFIRMATION',
                              'IN_REPAIR','PAUSED_FOR_REQUOTE','WAITING_ACCEPTANCE')
                 OR (w.status='WAITING_MANUAL_REVIEW' AND EXISTS (
                     SELECT 1 FROM work_order_status_log log
                     WHERE log.work_order_id=w.id AND log.to_status='APPOINTED'
                 )))
          </when>
        </choose>
        ORDER BY r.created_at DESC,r.id DESC
        </script>
        """)
    IPage<RepairRecordListItemVO> list(IPage<RepairRecordListItemVO> page,
            @Param("userId") Long userId, @Param("group") String group);
}
