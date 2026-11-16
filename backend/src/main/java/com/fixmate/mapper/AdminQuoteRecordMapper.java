package com.fixmate.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fixmate.dto.admin.StaffIdentityContracts.AdminQuoteRecordVO;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminQuoteRecordMapper {
    record Row(String id,Long workOrderId,String workOrderNo,String quoteNo,String quoteType,String status,
               String amount,String visitFeeAmount,LocalDateTime recordedAt) {
        public AdminQuoteRecordVO toView() {
            return new AdminQuoteRecordVO(id,workOrderId,workOrderNo,quoteNo,quoteType,status,amount,visitFeeAmount,
                    recordedAt.atOffset(ZoneOffset.UTC));
        }
    }

    @Select("""
        <script>
        SELECT entry.id, entry.work_order_id, entry.work_order_no, entry.quote_no,
               entry.quote_type, entry.status, entry.amount, entry.visit_fee_amount, entry.recorded_at
        FROM (
            SELECT CONCAT('quote-',q.id) AS id, w.id AS work_order_id, w.work_order_no, q.quote_no, q.quote_type,
                   CASE q.status WHEN 'ACCEPTED' THEN 'COMPLETED' WHEN 'SUBMITTED' THEN 'WAITING_QUOTE_CONFIRMATION'
                        ELSE q.status END AS status,
                   CAST(q.subtotal_amount AS CHAR) AS amount,
                   CAST((SELECT SUM(i.amount) FROM quote_item i WHERE i.quote_id=q.id AND i.fee_type='VISIT'
                         AND i.content_revision=COALESCE(q.item_revision,0)) AS CHAR) AS visit_fee_amount,
                   q.submitted_at AS recorded_at
            FROM quote q JOIN work_order w ON w.id=q.work_order_id
            WHERE q.submitted_at IS NOT NULL AND q.status IN ('SUBMITTED','ACCEPTED','REJECTED','WITHDRAWN','EXPIRED')
            UNION ALL
            SELECT CONCAT('fee-',w.id),w.id,w.work_order_no,NULL,'CANCELLATION_VISIT_FEE','VISIT_FEE_RECORDED',
                   CAST(w.cancellation_fee_amount AS CHAR),CAST(w.cancellation_fee_amount AS CHAR),w.updated_at
            FROM work_order w WHERE w.status='CANCELLED' AND w.cancellation_fee_amount IS NOT NULL
        ) entry
        <where>
            <if test="status != null">AND entry.status=#{status}</if>
            <if test="keyword != null">
              AND (entry.work_order_no LIKE CONCAT('%',#{keyword},'%') OR entry.quote_no LIKE CONCAT('%',#{keyword},'%'))
            </if>
        </where>
        ORDER BY entry.recorded_at DESC, entry.id DESC
        </script>
        """)
    IPage<Row> list(IPage<Row> page,@Param("status") String status,@Param("keyword") String keyword);
}
