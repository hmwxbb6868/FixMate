package com.fixmate.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fixmate.dto.user.RepairRecordListItemVO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminRepairRequestQueryMapper {
    @Select("""
        <script>
        SELECT r.id,r.repair_no,r.status AS repair_status,r.appliance_type,b.code AS brand_code,
               COALESCE(m.model_code,r.model_text) AS model_text,LEFT(r.symptom_description,120) AS symptom_summary,
               r.created_at,w.id AS work_order_id,w.status AS work_order_status,r.version
        FROM repair_request r
        LEFT JOIN work_order w ON w.repair_request_id=r.id AND w.customer_id=r.user_id
        LEFT JOIN appliance_brand b ON b.id=r.brand_id
        LEFT JOIN appliance_model m ON m.id=r.model_id
        <where>
          <if test="status != null">AND r.status=#{status}</if>
          <if test="repairNo != null">AND r.repair_no=#{repairNo} AND r.status!='DRAFT'</if>
          <if test="pendingReview">
            AND r.status IN ('SUBMITTED','HUMAN_HANDOFF') AND r.work_order_id IS NULL
            AND r.triage_session_id IS NULL AND w.id IS NULL
            AND NOT EXISTS (SELECT 1 FROM safety_event se WHERE se.repair_request_id=r.id AND se.status!='RESOLVED')
          </if>
          <if test="pendingCustomerConfirmation">
            AND (
                (r.status='TRIAGED' AND r.work_order_id IS NULL AND w.id IS NULL)
                OR (r.status='HIGH_RISK_BLOCKED' AND r.work_order_id=w.id AND w.status='WAITING_CUSTOMER_VISIT'
                    AND NOT EXISTS (SELECT 1 FROM safety_event se WHERE se.work_order_id=w.id AND se.status!='RESOLVED'))
            )
          </if>
        </where>
        ORDER BY r.created_at DESC,r.id DESC
        </script>
        """)
    IPage<RepairRecordListItemVO> list(IPage<RepairRecordListItemVO> page,@Param("status") String status,
                                     @Param("pendingReview") boolean pendingReview,@Param("repairNo") String repairNo,
                                     @Param("pendingCustomerConfirmation") boolean pendingCustomerConfirmation);
}
